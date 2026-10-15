package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.catch
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.TroubleshootingRepository
import com.example.data.local.UserSessionManager
import com.example.data.model.A2LCalculator
import com.example.data.model.A2LComplianceResult
import com.example.data.model.A2LRefrigerants
import com.example.data.model.DiagnosticMessage
import com.example.data.model.DiagnosticScenarios
import com.example.data.model.ErrorCode
import com.example.data.model.ErrorCodeDatabase
import com.example.data.model.MessageSender
import com.example.data.model.Refrigerant
import com.example.data.model.RefrigerantData
import com.example.data.model.TroubleshootingLog
import com.example.data.model.Customer
import com.example.data.model.Equipment
import com.example.data.model.EquipmentWithCustomer
import com.example.data.model.ServiceJob
import com.example.data.model.NameplateFields
import com.example.data.model.NameplateParser
import com.example.data.model.InspectionTemplate
import com.example.data.model.MaintenanceReminder
import com.example.data.model.UserTier
import com.example.data.model.VideoDiagnosticDatabase
import com.example.data.model.VideoDiagnosticSample
import com.example.data.remote.SeniorTechAiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.report.BackupPayload
import com.example.report.EncryptedBackupManager
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class HvacViewModel(application: Application) : AndroidViewModel(application) {

    private val userSessionManager = UserSessionManager(application)
    val currentTier: StateFlow<UserTier> = userSessionManager.currentTier
    val aiQueriesUsed: StateFlow<Int> = userSessionManager.aiQueriesUsed
    val weeklyVideoScansUsed: StateFlow<Int> = userSessionManager.weeklyVideoScansUsed
    val onboardingCompleted: StateFlow<Boolean> = userSessionManager.onboardingCompleted
    val companyName: StateFlow<String> = userSessionManager.companyName
    val companyContact: StateFlow<String> = userSessionManager.companyContact
    val reportTerms: StateFlow<String> = userSessionManager.reportTerms
    val companyLogoPath: StateFlow<String> = userSessionManager.companyLogoPath

    private val database = AppDatabase.getInstance(application)
    private val repository = TroubleshootingRepository(database.troubleshootingDao())
    private val fieldDao = database.fieldDao()

    val customers = fieldDao.customers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val equipment = fieldDao.equipment().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val equipmentOverview = fieldDao.equipmentOverview().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val serviceJobs = fieldDao.jobs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inspectionTemplates = fieldDao.templates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val maintenanceReminders = fieldDao.reminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val aiService = SeniorTechAiService()

    private val _operationError = MutableStateFlow<String?>(null)
    val operationError = _operationError.asStateFlow()
    private val _savingLog = MutableStateFlow(false)
    val savingLog = _savingLog.asStateFlow()
    private var diagnosticJob: Job? = null
    private var scanJob: Job? = null
    init {
        viewModelScope.launch {
            try {
                repository.ensureInitialData()
                if (fieldDao.allTemplates().isEmpty()) defaultTemplates().forEach { fieldDao.insertTemplate(it) }
            }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _operationError.value = "Could not open logbook. Restart and check storage." }
        }
    }
    fun clearOperationError() { _operationError.value = null }

    fun parseNameplate(text: String): NameplateFields = NameplateParser.parse(text)

    fun createEquipmentRecord(
        customerName: String, phone: String, address: String, nickname: String,
        manufacturer: String, model: String, serial: String, equipmentType: String,
        capacity: String, voltage: String, phase: String, refrigerant: String,
        onSaved: (Long) -> Unit = {}
    ) {
        if (customerName.isBlank() || nickname.isBlank()) { _operationError.value = "Customer and equipment name are required."; return }
        if (listOf(customerName, phone, address, nickname, manufacturer, model, serial, equipmentType, capacity, voltage, phase, refrigerant).any { it.length > 500 }) {
            _operationError.value = "One or more equipment fields are too long."; return
        }
        viewModelScope.launch {
            try {
                if (fieldDao.equipmentCount() >= currentTier.value.equipmentLimit) { _operationError.value = "Free includes 3 equipment records. Plus removes this limit."; return@launch }
                val id = fieldDao.insertCustomerAndEquipment(
                    Customer(name = customerName.trim(), phone = phone.trim(), address = address.trim()),
                    Equipment(customerId = 0, nickname = nickname.trim(), manufacturer = manufacturer.trim(), model = model.trim(), serial = serial.trim(), equipmentType = equipmentType.trim().ifBlank { "Split system" }, capacity = capacity.trim(), voltage = voltage.trim(), phase = phase.trim(), refrigerant = refrigerant.trim())
                )
                onSaved(id)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _operationError.value = "Equipment was not saved. Check the fields and retry." }
        }
    }

    fun createServiceJob(
        equipmentId: Long, complaint: String, workflowKey: String, readings: String,
        findings: String, workPerformed: String, partsUsed: String, recommendations: String,
        templateName: String = "", checklistResults: String = "", beforePhotoUri: String = "", afterPhotoUri: String = "",
        technicianSignature: String = "", customerSignature: String = "", reportTerms: String = "",
        onSaved: (Long) -> Unit = {}
    ) {
        if (equipmentId <= 0 || complaint.isBlank() || findings.isBlank() || workPerformed.isBlank()) {
            _operationError.value = "Choose equipment and add the complaint, findings and work performed."; return
        }
        if (listOf(complaint, readings, findings, workPerformed, partsUsed, recommendations).any { it.length > 8000 }) { _operationError.value = "Job details are too long."; return }
        viewModelScope.launch {
            try {
                if (fieldDao.jobCount() >= currentTier.value.jobLimit) { _operationError.value = "Free includes 10 completed jobs. Plus removes this limit."; return@launch }
                val id = fieldDao.insertJob(ServiceJob(equipmentId = equipmentId, complaint = complaint.trim(), workflowKey = workflowKey, readings = readings.trim(), findings = findings.trim(), workPerformed = workPerformed.trim(), partsUsed = partsUsed.trim(), recommendations = recommendations.trim(), templateName = templateName.trim(), checklistResults = checklistResults.trim(), beforePhotoUri = beforePhotoUri, afterPhotoUri = afterPhotoUri, technicianSignature = technicianSignature.trim().take(120), customerSignature = customerSignature.trim().take(120), reportTerms = reportTerms.trim().take(2000)))
                onSaved(id)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _operationError.value = "Job was not saved. Your entered information remains on screen." }
        }
    }

    suspend fun reportData(jobId: Long): Triple<Customer, Equipment, ServiceJob>? {
        val job = fieldDao.jobById(jobId) ?: return null
        val unit = fieldDao.equipmentById(job.equipmentId) ?: return null
        val customer = fieldDao.customerById(unit.customerId) ?: return null
        return Triple(customer, unit, job)
    }
    suspend fun exportData() = Triple(fieldDao.allCustomers(), fieldDao.allEquipment(), fieldDao.allJobs())
    suspend fun backupPayload() = BackupPayload(fieldDao.allCustomers(),fieldDao.allEquipment(),fieldDao.allJobs(),fieldDao.allTemplates(),fieldDao.allReminders())
    suspend fun createEncryptedBackup(password: CharArray): ByteArray = withContext(Dispatchers.Default) { EncryptedBackupManager.encrypt(backupPayload(), password) }
    suspend fun restoreEncryptedBackup(bytes: ByteArray, password: CharArray) {
        val payload=withContext(Dispatchers.Default) { EncryptedBackupManager.decrypt(bytes,password) }
        fieldDao.replaceAll(payload.customers,payload.equipment,payload.jobs,payload.templates.ifEmpty { defaultTemplates() },payload.reminders)
    }


    fun setTier(tier: UserTier) {
        userSessionManager.setTier(tier)
        if (!currentTier.value.canAccessSeniorTechAI) clearDiagnosticHistory()
        if (!currentTier.value.canAccessSchematicScanner) { scanJob?.cancel(); _isScanning.value = false; _scannerResult.value = null }
    }

    fun completeOnboarding() {
        userSessionManager.setOnboardingCompleted(true)
    }
    fun setCompanyName(value: String) = userSessionManager.setCompanyName(value)
    fun setBusinessIdentity(name: String, contact: String, terms: String, logoPath: String = companyLogoPath.value) = userSessionManager.setBusinessIdentity(name,contact,terms,logoPath)

    fun addInspectionTemplate(name: String, category: String, items: String) {
        if (!currentTier.value.canUseInspectionTemplates) return
        if (name.isBlank() || items.lineSequence().count { it.isNotBlank() } < 2) { _operationError.value="Add a template name and at least two checklist items."; return }
        viewModelScope.launch { runCatching { fieldDao.insertTemplate(InspectionTemplate(name=name.trim().take(80),category=category.trim().take(40).ifBlank{"Custom"},items=items.lineSequence().map{it.trim()}.filter{it.isNotBlank()}.take(50).joinToString("\n"))) }.onFailure { _operationError.value="Template could not be saved." } }
    }
    fun deleteInspectionTemplate(id: Long) { if(currentTier.value.canUseInspectionTemplates) viewModelScope.launch { fieldDao.deleteTemplate(id) } }
    fun addMaintenanceReminder(equipmentId: Long, title: String, notes: String, dueAt: Long) {
        if (!currentTier.value.canUseMaintenanceReminders) return
        if(equipmentId<=0 || title.isBlank() || dueAt<=0) { _operationError.value="Choose equipment, a reminder title and a valid date."; return }
        viewModelScope.launch { runCatching { fieldDao.insertReminder(MaintenanceReminder(equipmentId=equipmentId,title=title.trim().take(120),notes=notes.trim().take(1000),dueAt=dueAt)) }.onFailure { _operationError.value="Reminder could not be saved." } }
    }
    fun setReminderCompleted(id: Long, completed: Boolean) { if(currentTier.value.canUseMaintenanceReminders) viewModelScope.launch { fieldDao.setReminderCompleted(id,completed) } }

    private fun defaultTemplates() = listOf(
        InspectionTemplate(name="Preventive maintenance",category="Maintenance",items="Confirm disconnect and PPE\nInspect filters and airflow path\nRecord temperature split\nRecord operating pressures and temperatures\nInspect electrical connections\nDocument findings and recommendations",builtIn=true),
        InspectionTemplate(name="Repair verification",category="Repair",items="Record original complaint\nRecord confirmed cause\nDocument parts installed\nRecord final operating measurements\nVerify normal operation\nExplain result to customer",builtIn=true),
        InspectionTemplate(name="Startup / commissioning",category="Installation",items="Confirm model and serial\nVerify voltage and phase\nRecord airflow setup\nRecord refrigerant measurements\nVerify controls and safeties\nRecord final startup condition",builtIn=true)
    )

    // -------------------------------------------------------------------------
    // CALCULATORS STATE
    // -------------------------------------------------------------------------

    // Superheat
    var selectedShRefrigerant = MutableStateFlow(RefrigerantData.R410A)
    var shSuctionPressure = MutableStateFlow("118") // psig
    var shSuctionTemp = MutableStateFlow("52") // °F
    var shOutdoorDryBulb = MutableStateFlow("90") // °F (for target calculation)
    var shIndoorWetBulb = MutableStateFlow("63") // °F (for target calculation)
    var shUseFixedTarget = MutableStateFlow(false)
    var shFixedTarget = MutableStateFlow("12") // °F

    // Subcooling
    var selectedScRefrigerant = MutableStateFlow(RefrigerantData.R410A)
    var scLiquidPressure = MutableStateFlow("365") // psig
    var scLiquidTemp = MutableStateFlow("100") // °F
    var scTarget = MutableStateFlow("10") // °F

    // CFM
    var cfmMode = MutableStateFlow("Sensible Heat") // "Sensible Heat", "Cooling Tonnage", "Electric Strip"
    var cfmBtu = MutableStateFlow("36000")
    var cfmDeltaT = MutableStateFlow("20")
    var cfmTons = MutableStateFlow("3.0")
    var cfmCfmPerTon = MutableStateFlow("400")
    var cfmVolts = MutableStateFlow("240")
    var cfmAmps = MutableStateFlow("41.6")

    // Duct Sizing
    var ductCfm = MutableStateFlow("800")
    var ductFriction = MutableStateFlow("0.08") // in. w.c./100ft
    var ductRectWidth = MutableStateFlow("12") // Target width for rectangular equivalent

    // -------------------------------------------------------------------------
    // PT CHART STATE
    // -------------------------------------------------------------------------
    var ptSelectedRefrigerant = MutableStateFlow(RefrigerantData.R410A)
    var ptGaugePressure = MutableStateFlow("118") // psig

    // -------------------------------------------------------------------------
    // BRAIN DUMP / TROUBLESHOOTING LOG STATE
    // -------------------------------------------------------------------------
    private val _logSearchQuery = MutableStateFlow("")
    val logSearchQuery: StateFlow<String> = _logSearchQuery.asStateFlow()

    private val _logFilterBrand = MutableStateFlow("All")
    val logFilterBrand: StateFlow<String> = _logFilterBrand.asStateFlow()

    val troubleshootingLogs: StateFlow<List<TroubleshootingLog>> =
        combine(repository.allLogs, _logSearchQuery, _logFilterBrand) { logs, query, brand ->
            logs.filter { log ->
                val matchesQuery = query.isBlank() ||
                    log.title.contains(query, ignoreCase = true) ||
                    log.symptom.contains(query, ignoreCase = true) ||
                    log.brand.contains(query, ignoreCase = true) ||
                    log.errorCode.contains(query, ignoreCase = true) ||
                    log.resolution.contains(query, ignoreCase = true) || log.customerTag.contains(query, ignoreCase = true) || log.rootCause.contains(query, ignoreCase = true)

                val matchesBrand = brand == "All" || log.brand.contains(brand, ignoreCase = true)

                matchesQuery && matchesBrand
            }
        }.catch { error ->
            if (error is CancellationException) throw error
            _operationError.value = "Could not load logbook. Check storage and restart."
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLogSearchQuery(query: String) {
        _logSearchQuery.value = query
    }

    fun setLogFilterBrand(brand: String) {
        _logFilterBrand.value = brand
    }

    fun addTroubleshootingLog(title: String, brand: String, equipmentType: String, symptom: String,
        rootCause: String, resolution: String, errorCode: String, customerTag: String, onSaved: () -> Unit = {}) {
        if (_savingLog.value) return
        if (title.isBlank() || symptom.isBlank() || resolution.isBlank()) { _operationError.value = "Add a title, symptom and resolution."; return }
        if (title.length > 200 || brand.length > 100 || symptom.length > 4000 || rootCause.length > 4000 || resolution.length > 8000 || errorCode.length > 100 || customerTag.length > 300) { _operationError.value = "Note is too long. Shorten the title or details."; return }
        _savingLog.value = true
        viewModelScope.launch {
            try {
                repository.insertLog(TroubleshootingLog(title = title.trim(), brand = brand.trim().ifBlank { "Generic" },
                    equipmentType = equipmentType.trim().ifBlank { "Split System" }, symptom = symptom.trim(), rootCause = rootCause.trim(),
                    resolution = resolution.trim(), errorCode = errorCode.trim(), customerTag = customerTag.trim()))
                onSaved()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _operationError.value = "Not saved. Your draft is still open; check storage and retry." }
            finally { _savingLog.value = false }
        }
    }
    fun deleteLog(id: Long) { viewModelScope.launch {
        try { repository.deleteLogById(id) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { _operationError.value = "Could not delete log. Please retry." }
    } }
    fun toggleFavorite(log: TroubleshootingLog) { viewModelScope.launch {
        try { repository.toggleFavorite(log.id) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { _operationError.value = "Could not update favorite. Please retry." }
    } }

    // -------------------------------------------------------------------------
    // ERROR CODES SEARCH STATE
    // -------------------------------------------------------------------------
    private val _errorSearchQuery = MutableStateFlow("")
    val errorSearchQuery: StateFlow<String> = _errorSearchQuery.asStateFlow()

    private val _errorSelectedBrand = MutableStateFlow("All")
    val errorSelectedBrand: StateFlow<String> = _errorSelectedBrand.asStateFlow()

    val filteredErrorCodes: StateFlow<List<ErrorCode>> =
        combine(_errorSearchQuery, _errorSelectedBrand) { query, brand ->
            ErrorCodeDatabase.entries.filter { code ->
                val matchesQuery = query.isBlank() ||
                    code.codeDisplay.contains(query, ignoreCase = true) ||
                    code.faultTitle.contains(query, ignoreCase = true) ||
                    code.brand.contains(query, ignoreCase = true) ||
                    code.probableCauses.any { it.contains(query, ignoreCase = true) }

                val matchesBrand = brand == "All" || code.brand.contains(brand, ignoreCase = true)

                matchesQuery && matchesBrand
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ErrorCodeDatabase.entries)

    fun setErrorSearchQuery(query: String) {
        _errorSearchQuery.value = query
    }

    fun setErrorSelectedBrand(brand: String) {
        _errorSelectedBrand.value = brand
    }

    // -------------------------------------------------------------------------
    // A2L COMPLIANCE CALCULATOR STATE
    // -------------------------------------------------------------------------
    var a2lSelectedRefrigerant = MutableStateFlow(A2LRefrigerants.R454B)
    var a2lRoomArea = MutableStateFlow("350") // sq ft
    var a2lCeilingHeight = MutableStateFlow("8.5") // ft
    var a2lFactoryChargeLbs = MutableStateFlow("") // lbs
    var a2lLineSetLengthFt = MutableStateFlow("") // ft
    var a2lLineOzPerFt = MutableStateFlow("") // oz/ft

    fun getA2LComplianceResult(): A2LComplianceResult {
        val area = number(a2lRoomArea.value, "Room area", 0.1..1000000.0)
        val height = number(a2lCeilingHeight.value, "Ceiling height", 0.1..1000.0)
        val factoryCharge = number(a2lFactoryChargeLbs.value, "Factory charge", 0.0..100000.0)
        val lineLength = number(a2lLineSetLengthFt.value, "Additional line length", 0.0..10000.0)
        val ozPerFt = number(a2lLineOzPerFt.value, "Line charge rate", 0.0..1000.0)

        return A2LCalculator.calculate(
            refrigerant = a2lSelectedRefrigerant.value,
            areaSqFt = area,
            heightFt = height,
            factoryChargeLbs = factoryCharge,
            lineLengthFt = lineLength,
            extraChargeOzPerFt = ozPerFt
        )
    }

    // -------------------------------------------------------------------------
    // SENIOR TECH AI MENTOR STATE
    // -------------------------------------------------------------------------
    private val _diagnosticMessages = MutableStateFlow<List<DiagnosticMessage>>(
        listOf(
            DiagnosticMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.SENIOR_TECH_AI,
                text = "Choose a topic or describe a symptom to find a keyword-based offline checklist. This is not conversational AI or an equipment diagnosis.",
                quickActions = listOf(
                    "Compressor start issue",
                    "Superheat checks",
                    "Furnace lockout"
                )
            )
        )
    )
    val diagnosticMessages: StateFlow<List<DiagnosticMessage>> = _diagnosticMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    fun sendDiagnosticQuery(userQuery: String) {
        if (!currentTier.value.canAccessSeniorTechAI || userQuery.isBlank() || _isAiThinking.value) return
        if (userQuery.length > 4000) { _operationError.value = "Keep descriptions under 4,000 characters."; return }

        val userMsg = DiagnosticMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.TECHNICIAN,
            text = userQuery
        )

        _diagnosticMessages.value = _diagnosticMessages.value + userMsg
        _isAiThinking.value = true
        userSessionManager.incrementAiQuery()

        diagnosticJob = viewModelScope.launch {
            try {
                val response = aiService.diagnoseFieldIssue(userQuery.trim())
                _diagnosticMessages.value = (_diagnosticMessages.value + DiagnosticMessage(UUID.randomUUID().toString(), MessageSender.SENIOR_TECH_AI, response)).takeLast(100)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _operationError.value = "Could not load guide. Retry." }
            finally { _isAiThinking.value = false }
        }
    }

    fun clearDiagnosticHistory() {
        diagnosticJob?.cancel(); _isAiThinking.value = false
        _diagnosticMessages.value = listOf(
            DiagnosticMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.SENIOR_TECH_AI,
                text = "Guide cleared. Choose a checklist topic. This offline guide does not remember previous messages.",
                quickActions = listOf(
                    "Compressor buzzing on start",
                    "Reversing valve stuck",
                    "Frozen evap coil checklist"
                )
            )
        )
    }

    // -------------------------------------------------------------------------
    // SCHEMATIC & PART SCANNER STATE
    // -------------------------------------------------------------------------
    private val _scannerResult = MutableStateFlow<String?>(null)
    val scannerResult: StateFlow<String?> = _scannerResult.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun runSchematicScan(bitmap: Bitmap?, presetSample: String = "") {
        if (!currentTier.value.canAccessSchematicScanner || _isScanning.value) return
        scanJob = viewModelScope.launch {
            _isScanning.value = true; _scannerResult.value = null
            try { _scannerResult.value = withTimeout(30000) { aiService.scanSchematicOrDataPlate(bitmap, presetSample) } }
            catch (e: kotlinx.coroutines.TimeoutCancellationException) { _scannerResult.value = "Timed out. Try a clearer photo." }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _scannerResult.value = e.message ?: "Could not read label." }
            finally { _isScanning.value = false }
        }
    }
    fun scanLabel(uri: Uri) {
        if (!currentTier.value.canAccessSchematicScanner || _isScanning.value) return
        _isScanning.value = true; _scannerResult.value = null
        scanJob = viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    resolver.openAssetFileDescriptor(uri, "r")?.use { require(it.length <= 25L * 1024 * 1024) { "Choose an image under 25 MB." } }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    require(bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth.toLong() * bounds.outHeight <= 50000000L) { "Choose a valid image up to 50 megapixels." }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
                    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: error("Image could not be opened.")
                    val orientation = runCatching { resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) } }.getOrNull()
                    val matrix = Matrix()
                    when (orientation) {
                        6 -> matrix.postRotate(90f)
                        3 -> matrix.postRotate(180f)
                        8 -> matrix.postRotate(270f)
                        2 -> matrix.postScale(-1f, 1f)
                        4 -> matrix.postScale(1f, -1f)
                        5 -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                        7 -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
                    }
                    if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also { if (it !== decoded) decoded.recycle() }
                }
                _scannerResult.value = withTimeout(30000) { aiService.scanSchematicOrDataPlate(bitmap) }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) { _scannerResult.value = "Timed out. Try another photo." }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _scannerResult.value = e.message ?: "Could not read photo." }
            finally { _isScanning.value = false }
        }
    }
    fun clearScanResult() { _scannerResult.value = null }

    // -------------------------------------------------------------------------
    // VIDEO DIAGNOSTICS STATE (Weekly 3 Free Video Scans for Free Tier)
    // -------------------------------------------------------------------------
    private val _isVideoScanning = MutableStateFlow(false)
    val isVideoScanning: StateFlow<Boolean> = _isVideoScanning.asStateFlow()

    private val _videoScanResult = MutableStateFlow<VideoDiagnosticSample?>(null)
    val videoScanResult: StateFlow<VideoDiagnosticSample?> = _videoScanResult.asStateFlow()

    fun runVideoDiagnostic(sample: VideoDiagnosticSample) { _videoScanResult.value = sample }

    fun clearVideoScanResult() {
        _videoScanResult.value = null
    }

    fun remainingWeeklyVideoScans(): Int {
        userSessionManager.refreshUsageWindow()
        val maxAllowed = currentTier.value.weeklyVideoScansAllowed
        val used = weeklyVideoScansUsed.value
        return (maxAllowed - used).coerceAtLeast(0)
    }

    // -------------------------------------------------------------------------
    // CALCULATOR HELPERS
    // -------------------------------------------------------------------------

    private fun number(text: String, label: String, range: ClosedFloatingPointRange<Double>): Double {
        val value = text.trim().toDoubleOrNull()
        require(value != null && value.isFinite() && value in range) { "$label: enter a number from ${range.start} to ${range.endInclusive}." }
        return value
    }
    // Superheat calculations
    fun calculateSuperheat(): SuperheatResult {
        val ref = selectedShRefrigerant.value
        val p = number(shSuctionPressure.value, "Suction pressure", -14.6..1000.0)
        val actualTemp = number(shSuctionTemp.value, "Suction temperature", -150.0..300.0)
        val evapSatTemp = ref.satTempFromPsig(p)
        val actualSH = actualTemp - evapSatTemp

        val targetSH: Double
        if (shUseFixedTarget.value) {
            targetSH = number(shFixedTarget.value, "Target superheat", 0.0..100.0)
        } else {
            // Standard Piston / Non-TXV Target Superheat formula:
            // Target SH = ( (3 * Indoor WB) - 80 - Outdoor DB ) / 2
            val wb = number(shIndoorWetBulb.value, "Indoor wet bulb", 30.0..100.0)
            val db = number(shOutdoorDryBulb.value, "Outdoor dry bulb", -50.0..150.0)
            val calculatedTarget = ((3.0 * wb) - 80.0 - db) / 2.0
            require(calculatedTarget in 5.0..30.0) { "Conditions outside target formula range. Use equipment charging chart." }
            targetSH = calculatedTarget
        }

        val diff = actualSH - targetSH
        val diagnosis = when {
            diff > 5.0 -> SuperheatVerdict.TOO_HIGH
            diff < -5.0 -> SuperheatVerdict.TOO_LOW
            else -> SuperheatVerdict.IN_TARGET
        }

        return SuperheatResult(
            evapSatTempF = (evapSatTemp * 10).roundToInt() / 10.0,
            actualSuperheatF = (actualSH * 10).roundToInt() / 10.0,
            targetSuperheatF = (targetSH * 10).roundToInt() / 10.0,
            verdict = diagnosis
        )
    }

    // Subcooling calculations
    fun calculateSubcooling(): SubcoolingResult {
        val ref = selectedScRefrigerant.value
        require(ref.glideF <= 0.2) { "This blend needs a verified bubble-point table for subcooling." }
        val p = number(scLiquidPressure.value, "Liquid pressure", 0.0..1000.0)
        val actualTemp = number(scLiquidTemp.value, "Liquid temperature", -150.0..300.0)
        val condSatTemp = ref.satTempFromPsig(p)
        val actualSC = condSatTemp - actualTemp
        val targetSC = number(scTarget.value, "Target subcooling", 0.0..100.0)

        val diff = actualSC - targetSC
        val verdict = when {
            diff > 3.0 -> SubcoolingVerdict.TOO_HIGH
            diff < -3.0 -> SubcoolingVerdict.TOO_LOW
            else -> SubcoolingVerdict.IN_TARGET
        }

        return SubcoolingResult(
            condSatTempF = (condSatTemp * 10).roundToInt() / 10.0,
            actualSubcoolingF = (actualSC * 10).roundToInt() / 10.0,
            targetSubcoolingF = (targetSC * 10).roundToInt() / 10.0,
            verdict = verdict
        )
    }

    // CFM calculations
    fun calculateCfm(): CfmResult {
        return when (cfmMode.value) {
            "Sensible Heat" -> {
                val btu = number(cfmBtu.value, "Sensible capacity", 0.1..10000000.0)
                val dt = number(cfmDeltaT.value, "Temperature difference", 0.1..300.0)
                // CFM = BTU / (1.08 * ΔT)
                val cfm = btu / (1.08 * dt)
                CfmResult(cfm = cfm.roundToInt(), formula = "CFM = BTU / (1.08 × ΔT)")
            }
            "Electric Strip" -> {
                val volts = number(cfmVolts.value, "Voltage", 0.1..1000.0)
                val amps = number(cfmAmps.value, "Current", 0.1..10000.0)
                val dt = number(cfmDeltaT.value, "Temperature difference", 0.1..300.0)
                // CFM = (Volts * Amps * 3.413) / (1.08 * ΔT)
                val cfm = (volts * amps * 3.413) / (1.08 * dt)
                CfmResult(cfm = cfm.roundToInt(), formula = "CFM = (V × A × 3.413) / (1.08 × ΔT)")
            }
            else -> {
                // Cooling Tonnage
                val tons = number(cfmTons.value, "Tonnage", 0.1..10000.0)
                val perTon = number(cfmCfmPerTon.value, "CFM per ton", 1.0..2000.0)
                val cfm = tons * perTon
                CfmResult(cfm = cfm.roundToInt(), formula = "CFM = Tons × CFM per ton (design estimate)")
            }
        }
    }

    // Duct Sizing calculations
    fun calculateDuct(): DuctResult {
        val cfm = number(ductCfm.value, "Airflow", 10.0..1000000.0)
        val friction = number(ductFriction.value, "Friction rate", 0.01..2.0)
        val targetWidth = number(ductRectWidth.value, "Duct width", 4.0..200.0)

        // Equal Friction Method approximation:
        // Round diameter D = 0.548 * (CFM^0.38) / (friction^0.19)
        val roundDiameter = 0.548 * (cfm.pow(0.38)) / (friction.pow(0.19))

        // Rectangular equivalent height calculation:
        // Area approx = pi * (D/2)^2
        val areaSqIn = Math.PI * (roundDiameter / 2.0).pow(2.0)
        val rectHeight = areaSqIn / targetWidth
        require(rectHeight >= 4.0) { "Selected width gives a height below 4 inches. Reduce width or use a duct design method." }

        // Velocity in FPM = CFM / Area_sq_ft
        val areaSqFt = areaSqIn / 144.0
        val velocityFpm = cfm / areaSqFt

        val noiseWarning = when {
            velocityFpm > 1000.0 -> "Velocity above 1000 FPM; assess noise and pressure losses for this installation."
            velocityFpm > 850.0 -> "Velocity 850–1000 FPM; suitability and noise depend on the installation."
            else -> "Lower velocity estimate; noise also depends on installation."
        }

        return DuctResult(
            roundDiameterInches = (roundDiameter * 10).roundToInt() / 10.0,
            rectWidthInches = targetWidth.roundToInt(),
            rectHeightInches = (rectHeight * 10).roundToInt() / 10.0,
            velocityFpm = velocityFpm.roundToInt(),
            velocityAssessment = noiseWarning
        )
    }
}

data class SuperheatResult(
    val evapSatTempF: Double,
    val actualSuperheatF: Double,
    val targetSuperheatF: Double,
    val verdict: SuperheatVerdict
)

enum class SuperheatVerdict {
    IN_TARGET,
    TOO_HIGH, // Undercharged or TXV restriction
    TOO_LOW   // Overcharged or TXV flooding
}

data class SubcoolingResult(
    val condSatTempF: Double,
    val actualSubcoolingF: Double,
    val targetSubcoolingF: Double,
    val verdict: SubcoolingVerdict
)

enum class SubcoolingVerdict {
    IN_TARGET,
    TOO_HIGH, // Overcharged or liquid line restriction
    TOO_LOW   // Undercharged
}

data class CfmResult(
    val cfm: Int,
    val formula: String
)

data class DuctResult(
    val roundDiameterInches: Double,
    val rectWidthInches: Int,
    val rectHeightInches: Double,
    val velocityFpm: Int,
    val velocityAssessment: String
)
