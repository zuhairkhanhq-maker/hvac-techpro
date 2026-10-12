package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.DiagnosticWorkflows
import com.example.data.model.EquipmentWithCustomer
import com.example.data.model.UserTier
import com.example.report.ServiceReportWriter
import com.example.report.PortableExportWriter
import com.example.ui.HvacViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.io.File

@Composable
fun LaunchHomeScreen(viewModel: HvacViewModel, onNewJob: () -> Unit, onEquipment: () -> Unit, onPlans: () -> Unit) {
    val jobs by viewModel.serviceJobs.collectAsState(); val equipment by viewModel.equipmentOverview.collectAsState(); val tier by viewModel.currentTier.collectAsState()
    LazyColumn(Modifier.fillMaxSize().testTag("launch_home"), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Ready for the next call", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Private field records and reports that keep working offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Button(onClick = onNewJob, Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("new_job")) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("New service job") } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Equipment", equipment.size.toString(), Modifier.weight(1f).clickable(onClick = onEquipment))
                MetricCard("Completed jobs", jobs.size.toString(), Modifier.weight(1f))
            }
        }
        item { Text("Recent work", style = MaterialTheme.typography.titleLarge) }
        if (jobs.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Text("No completed jobs yet. Start one and the record will stay on this device.", Modifier.padding(18.dp)) } }
        items(jobs.take(5), key = { it.id }) { job ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(job.complaint, fontWeight = FontWeight.SemiBold)
                Text(DateFormat.getDateInstance().format(Date(job.startedAt)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (job.findings.isNotBlank()) Text(job.findings, maxLines = 2)
            } }
        }
        item { OutlinedButton(onClick = onPlans, Modifier.fillMaxWidth()) { Text("${tier.title} · View plan") } }
    }
}

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(16.dp)) { Text(value, style = MaterialTheme.typography.headlineMedium); Text(label) } }
}

@Composable
fun EquipmentRecordsScreen(viewModel: HvacViewModel) {
    val records by viewModel.equipmentOverview.collectAsState(); val jobs by viewModel.serviceJobs.collectAsState(); var add by rememberSaveable { mutableStateOf(false) }; var selectedId by rememberSaveable { mutableLongStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Equipment history", style = MaterialTheme.typography.headlineSmall); Text("Every job stays attached to the correct unit.") }
            if (records.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Text("Add the first unit manually or read its nameplate.", Modifier.padding(18.dp)) } }
            items(records, key = { it.id }) { record -> EquipmentCard(record, { selectedId = record.id }) }
        }
        FloatingActionButton(onClick = { add = true }, Modifier.padding(20.dp).align(androidx.compose.ui.Alignment.BottomEnd).testTag("add_equipment")) { Icon(Icons.Default.Add, "Add equipment") }
    }
    if (add) AddEquipmentDialog(viewModel) { add = false }
    records.firstOrNull { it.id == selectedId }?.let { record -> AlertDialog(onDismissRequest = { selectedId = 0 }, title = { Text(record.nickname) }, text = { LazyColumn(Modifier.heightIn(max=460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("${record.customerName}\n${record.manufacturer} ${record.model}\nSerial: ${record.serial.ifBlank { "Not recorded" }}") }; val history=jobs.filter { it.equipmentId==record.id }; if(history.isEmpty()) item { Text("No service history yet.") } else items(history,key={it.id}) { job -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(job.complaint,fontWeight=FontWeight.Bold); Text(DateFormat.getDateInstance().format(Date(job.startedAt))); Text(job.findings,maxLines=3); Text(job.workPerformed,maxLines=3) } } } } }, confirmButton = { TextButton(onClick = { selectedId=0 }) { Text("Close") } }) }
}

@Composable private fun EquipmentCard(record: EquipmentWithCustomer, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick=onClick)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(record.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(record.customerName)
        Text(listOf(record.manufacturer, record.model).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Equipment details not added" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${record.jobCount} service ${if (record.jobCount == 1) "job" else "jobs"} · ${record.refrigerant.ifBlank { "Refrigerant not recorded" }}", style = MaterialTheme.typography.bodySmall)
    } }
}

@Composable
private fun AddEquipmentDialog(viewModel: HvacViewModel, dismiss: () -> Unit) {
    var customer by rememberSaveable { mutableStateOf("") }; var phone by rememberSaveable { mutableStateOf("") }; var address by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }; var maker by rememberSaveable { mutableStateOf("") }; var model by rememberSaveable { mutableStateOf("") }; var serial by rememberSaveable { mutableStateOf("") }
    var voltage by rememberSaveable { mutableStateOf("") }; var phase by rememberSaveable { mutableStateOf("") }; var refrigerant by rememberSaveable { mutableStateOf("") }; var capacity by rememberSaveable { mutableStateOf("") }
    val tier by viewModel.currentTier.collectAsState(); val scan by viewModel.scannerResult.collectAsState(); val scanning by viewModel.isScanning.collectAsState()
    val context = LocalContext.current; var captureUri by rememberSaveable { mutableStateOf<String?>(null) }; var captureError by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let(viewModel::scanLabel) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> captureUri?.let(Uri::parse)?.takeIf { ok }?.let(viewModel::scanLabel); captureUri=null }
    LaunchedEffect(scan) { scan?.let { parsed -> viewModel.parseNameplate(parsed).also { f -> maker = f.manufacturer.ifBlank { maker }; model = f.model.ifBlank { model }; serial = f.serial.ifBlank { serial }; voltage = f.voltage.ifBlank { voltage }; phase = f.phase.ifBlank { phase }; refrigerant = f.refrigerant.ifBlank { refrigerant }; capacity = f.capacity.ifBlank { capacity } } } }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Add equipment") }, text = {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tier in listOf(UserTier.PLUS_USER, UserTier.PRO_USER)) item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) { Button(onClick = { captureError=null; runCatching { val dir=File(context.cacheDir,"label_photos").apply{mkdirs()}; val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",File.createTempFile("label_",".jpg",dir)); captureUri=uri.toString(); camera.launch(uri) }.onFailure { captureError="Camera unavailable. Choose a saved photo." } }, enabled = !scanning, modifier = Modifier.weight(1f)) { Text("Take photo") }; OutlinedButton(onClick = { captureError=null; runCatching { picker.launch("image/*") }.onFailure { captureError="Image picker unavailable." } }, enabled = !scanning, modifier = Modifier.weight(1f)) { Text("Choose photo") } } }
            captureError?.let { message -> item { Text(message,color=MaterialTheme.colorScheme.error) } }
            item { Text("Confirm every extracted field before saving.", style = MaterialTheme.typography.bodySmall) }
            item { Field("Customer name *", customer) { customer = it } }; item { Field("Phone", phone) { phone = it } }; item { Field("Address", address) { address = it } }
            item { Field("Equipment name *", nickname) { nickname = it } }; item { Field("Manufacturer", maker) { maker = it } }; item { Field("Model", model) { model = it } }; item { Field("Serial", serial) { serial = it } }
            item { Field("Voltage", voltage) { voltage = it } }; item { Field("Phase", phase) { phase = it } }; item { Field("Refrigerant", refrigerant) { refrigerant = it } }; item { Field("Capacity", capacity) { capacity = it } }
        }
    }, confirmButton = { Button(enabled = customer.isNotBlank() && nickname.isNotBlank(), onClick = { viewModel.createEquipmentRecord(customer, phone, address, nickname, maker, model, serial, "Split system", capacity, voltage, phase, refrigerant) { dismiss() } }) { Text("Save equipment") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable private fun Field(label: String, value: String, onChange: (String) -> Unit) { OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true) }

@Composable
fun NewServiceJobScreen(viewModel: HvacViewModel, onDone: () -> Unit) {
    val units by viewModel.equipmentOverview.collectAsState(); val tier by viewModel.currentTier.collectAsState(); val company by viewModel.companyName.collectAsState(); val context = LocalContext.current; val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableLongStateOf(0) }; var menu by remember { mutableStateOf(false) }; var workflowKey by rememberSaveable { mutableStateOf("general") }; var workflowMenu by remember { mutableStateOf(false) }
    var complaint by rememberSaveable { mutableStateOf("") }; var readings by rememberSaveable { mutableStateOf("") }; var findings by rememberSaveable { mutableStateOf("") }; var work by rememberSaveable { mutableStateOf("") }; var parts by rememberSaveable { mutableStateOf("") }; var recs by rememberSaveable { mutableStateOf("") }
    val workflow = DiagnosticWorkflows.all.firstOrNull { it.key == workflowKey }
    LazyColumn(Modifier.fillMaxSize().testTag("new_job_form"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("New service job", style = MaterialTheme.typography.headlineSmall); Text("Required fields are saved only after you confirm the work.") }
        if (units.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Text("Add equipment first from the Equipment tab.", Modifier.padding(18.dp)) } }
        item { Box { OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth()) { Text(units.firstOrNull { it.id == selected }?.let { "${it.customerName} · ${it.nickname}" } ?: "Choose equipment *") }; DropdownMenu(menu, { menu = false }) { units.forEach { unit -> DropdownMenuItem({ Text("${unit.customerName} · ${unit.nickname}") }, { selected = unit.id; menu = false }) } } } }
        item { Field("Reported complaint *", complaint) { complaint = it } }
        item { Box { OutlinedButton(onClick = { workflowMenu = true }, Modifier.fillMaxWidth()) { Text(workflow?.title ?: "General inspection") }; DropdownMenu(workflowMenu, { workflowMenu = false }) { DropdownMenuItem({ Text("General inspection") }, { workflowKey = "general"; workflowMenu = false }); DiagnosticWorkflows.all.forEach { item -> DropdownMenuItem({ Text(item.title) }, { workflowKey = item.key; workflowMenu = false }) } } } }
        workflow?.let { selectedFlow -> item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Safety", fontWeight = FontWeight.Bold); Text(selectedFlow.safety); selectedFlow.steps.forEachIndexed { i, step -> Text("${i + 1}. $step") } } } } }
        item { MultiField("Measurements and operating conditions", readings) { readings = it } }; item { MultiField("Confirmed findings *", findings) { findings = it } }; item { MultiField("Work performed *", work) { work = it } }; item { MultiField("Parts used", parts) { parts = it } }; item { MultiField("Recommendations", recs) { recs = it } }
        item { Button(enabled = selected > 0 && complaint.isNotBlank() && findings.isNotBlank() && work.isNotBlank(), onClick = { viewModel.createServiceJob(selected, complaint, workflowKey, readings, findings, work, parts, recs) { id -> if (tier.canCreateReports) scope.launch { viewModel.reportData(id)?.let { (c, e, j) -> ServiceReportWriter.share(context, ServiceReportWriter.create(context, c, e, j, if (tier.canUseBranding) company else "HVAC TechPro Service")) }; onDone() } else onDone() } }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("save_job")) { Icon(Icons.Default.Description, null); Spacer(Modifier.width(8.dp)); Text(if (tier.canCreateReports) "Save and share report" else "Save job") } }
        item { Text(if (tier.canCreateReports) "The PDF is created locally and shared only when you choose an app." else "Plus adds unlimited records and offline PDF reports.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable private fun MultiField(label: String, value: String, onChange: (String) -> Unit) { OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, minLines = 3, maxLines = 6) }

@Composable
fun ReferenceHubScreen(viewModel: HvacViewModel, onErrorCodes: () -> Unit, onGuide: () -> Unit, onA2L: () -> Unit) {
    val tier by viewModel.currentTier.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Offline reference", style = MaterialTheme.typography.headlineSmall); Text("Checklists support—never replace—the exact manufacturer procedure.") }
        items(DiagnosticWorkflows.all) { flow -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(flow.title, fontWeight = FontWeight.Bold); Text(flow.steps.first()) } } }
        item { OutlinedButton(onClick = onGuide, Modifier.fillMaxWidth()) { Text("Open troubleshooting guide") } }
        item { OutlinedButton(onClick = onErrorCodes, enabled = tier.canAccessErrorCodes, modifier = Modifier.fillMaxWidth()) { Text(if (tier.canAccessErrorCodes) "Open fault-code reference" else "Fault-code reference · Plus") } }
        item { OutlinedButton(onClick = onA2L, enabled = tier.canAccessA2LCompliance, modifier = Modifier.fillMaxWidth()) { Text(if (tier.canAccessA2LCompliance) "Open A2L charge worksheet" else "A2L charge worksheet · Plus") } }
    }
}

@Composable
fun LaunchSettingsScreen(viewModel: HvacViewModel, onPlans: () -> Unit) {
    val tier by viewModel.currentTier.collectAsState()
    val company by viewModel.companyName.collectAsState(); var companyDraft by remember(company) { mutableStateOf(company) }
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val customers by viewModel.customers.collectAsState(); val equipment by viewModel.equipment.collectAsState(); val jobs by viewModel.serviceJobs.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(tier.title, style = MaterialTheme.typography.titleLarge); Text(tier.tagLine); Button(onClick = onPlans, Modifier.fillMaxWidth()) { Text("View plans") } } } }
        if (tier.canUseBranding) item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Pro report branding", fontWeight = FontWeight.Bold); Field("Company name", companyDraft) { companyDraft = it }; Button(onClick = { viewModel.setCompanyName(companyDraft) }) { Text("Save branding") } } } }
        if (tier.canUseBranding) item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Local business summary",fontWeight=FontWeight.Bold); Text("${jobs.size} jobs · ${equipment.size} equipment records · ${customers.size} customer records"); Text("Calculated on this device; no tracking or cloud analytics.",style=MaterialTheme.typography.bodySmall) } } }
        if (tier.canCreateReports) item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Portable export", fontWeight = FontWeight.Bold); Text("Create a local JSON copy of customers, equipment and jobs. The destination app you choose controls the exported file."); OutlinedButton(onClick = { scope.launch { val (c,e,j)=viewModel.exportData(); PortableExportWriter.share(context,PortableExportWriter.create(context,c,e,j)) } }, Modifier.fillMaxWidth()) { Text("Export my records") } } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Private by design", fontWeight = FontWeight.Bold); Text("Jobs, equipment, photos and reports remain on this device. The app does not request internet access for field work.") } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Backup before uninstalling", fontWeight = FontWeight.Bold); Text("Android may erase local records when the app is removed. Encrypted portable backup is a launch gate and is not represented as available yet.") } } }
        item { Text("Technical references and guided checks are support tools. Follow equipment documentation, regulations and qualified safety procedures.", style = MaterialTheme.typography.bodySmall) }
    }
}
