package com.example.report

import android.util.Base64
import com.example.data.model.Customer
import com.example.data.model.Equipment
import com.example.data.model.InspectionTemplate
import com.example.data.model.MaintenanceReminder
import com.example.data.model.ServiceJob
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupPayload(
    val customers: List<Customer>, val equipment: List<Equipment>, val jobs: List<ServiceJob>,
    val templates: List<InspectionTemplate>, val reminders: List<MaintenanceReminder>
)

object EncryptedBackupManager {
    private const val FORMAT = "HVACTECHPRO-BACKUP-V1"
    private const val ITERATIONS = 120_000

    fun encrypt(payload: BackupPayload, password: CharArray): ByteArray {
        require(password.size >= 8) { "Use a backup password with at least 8 characters." }
        val plain = toJson(payload).toByteArray(Charsets.UTF_8)
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(FORMAT.toByteArray())
        val root = JSONObject()
            .put("format", FORMAT).put("kdf", "PBKDF2-HMAC-SHA256").put("iterations", ITERATIONS)
            .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(cipher.doFinal(plain), Base64.NO_WRAP))
        password.fill('\u0000')
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    fun decrypt(bytes: ByteArray, password: CharArray): BackupPayload {
        require(bytes.size in 32..20_000_000) { "Backup file size is invalid." }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.getString("format") == FORMAT && root.getInt("iterations") == ITERATIONS) { "This is not a supported HVAC TechPro backup." }
        val salt = Base64.decode(root.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(root.getString("iv"), Base64.NO_WRAP)
        require(salt.size == 16 && iv.size == 12) { "Backup security header is invalid." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(FORMAT.toByteArray())
        val plain = cipher.doFinal(Base64.decode(root.getString("ciphertext"), Base64.NO_WRAP))
        password.fill('\u0000')
        return fromJson(plain.toString(Charsets.UTF_8))
    }

    private fun derive(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword(); return SecretKeySpec(encoded, "AES")
    }

    private fun toJson(p: BackupPayload): String = JSONObject().apply {
        put("schema", 1); put("createdAt", System.currentTimeMillis())
        put("customers", JSONArray().apply { p.customers.forEach { x -> put(JSONObject().put("id",x.id).put("name",x.name).put("phone",x.phone).put("address",x.address).put("createdAt",x.createdAt)) } })
        put("equipment", JSONArray().apply { p.equipment.forEach { x -> put(JSONObject().put("id",x.id).put("customerId",x.customerId).put("nickname",x.nickname).put("manufacturer",x.manufacturer).put("model",x.model).put("serial",x.serial).put("equipmentType",x.equipmentType).put("capacity",x.capacity).put("voltage",x.voltage).put("phase",x.phase).put("refrigerant",x.refrigerant).put("meteringDevice",x.meteringDevice).put("installedOn",x.installedOn).put("nameplateUri",x.nameplateUri).put("privateNotes",x.privateNotes).put("createdAt",x.createdAt)) } })
        put("jobs", JSONArray().apply { p.jobs.forEach { x -> put(JSONObject().put("id",x.id).put("equipmentId",x.equipmentId).put("complaint",x.complaint).put("workflowKey",x.workflowKey).put("readings",x.readings).put("findings",x.findings).put("workPerformed",x.workPerformed).put("partsUsed",x.partsUsed).put("recommendations",x.recommendations).put("templateName",x.templateName).put("checklistResults",x.checklistResults).put("beforePhotoUri",x.beforePhotoUri).put("afterPhotoUri",x.afterPhotoUri).put("technicianSignature",x.technicianSignature).put("customerSignature",x.customerSignature).put("reportTerms",x.reportTerms).put("status",x.status).put("startedAt",x.startedAt).put("completedAt",x.completedAt)) } })
        put("templates", JSONArray().apply { p.templates.forEach { x -> put(JSONObject().put("id",x.id).put("name",x.name).put("category",x.category).put("items",x.items).put("builtIn",x.builtIn).put("createdAt",x.createdAt)) } })
        put("reminders", JSONArray().apply { p.reminders.forEach { x -> put(JSONObject().put("id",x.id).put("equipmentId",x.equipmentId).put("title",x.title).put("notes",x.notes).put("dueAt",x.dueAt).put("completed",x.completed).put("createdAt",x.createdAt)) } })
    }.toString()

    private fun fromJson(raw: String): BackupPayload {
        val root = JSONObject(raw); require(root.getInt("schema") == 1) { "Unsupported backup schema." }
        fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
        val customers = root.getJSONArray("customers").objects().map { Customer(it.getLong("id"),it.getString("name"),it.optString("phone"),it.optString("address"),it.getLong("createdAt")) }
        val equipment = root.getJSONArray("equipment").objects().map { Equipment(it.getLong("id"),it.getLong("customerId"),it.getString("nickname"),it.optString("manufacturer"),it.optString("model"),it.optString("serial"),it.optString("equipmentType","Split system"),it.optString("capacity"),it.optString("voltage"),it.optString("phase"),it.optString("refrigerant"),it.optString("meteringDevice"),it.optString("installedOn"),it.optString("nameplateUri"),it.optString("privateNotes"),it.getLong("createdAt")) }
        val jobs = root.getJSONArray("jobs").objects().map { ServiceJob(it.getLong("id"),it.getLong("equipmentId"),it.getString("complaint"),it.optString("workflowKey","general"),it.optString("readings"),it.optString("findings"),it.optString("workPerformed"),it.optString("partsUsed"),it.optString("recommendations"),it.optString("templateName"),it.optString("checklistResults"),it.optString("beforePhotoUri"),it.optString("afterPhotoUri"),it.optString("technicianSignature"),it.optString("customerSignature"),it.optString("reportTerms"),it.optString("status","COMPLETE"),it.getLong("startedAt"),if(it.isNull("completedAt")) null else it.getLong("completedAt")) }
        val templates = root.getJSONArray("templates").objects().map { InspectionTemplate(it.getLong("id"),it.getString("name"),it.getString("category"),it.getString("items"),it.optBoolean("builtIn"),it.getLong("createdAt")) }
        val reminders = root.getJSONArray("reminders").objects().map { MaintenanceReminder(it.getLong("id"),it.getLong("equipmentId"),it.getString("title"),it.optString("notes"),it.getLong("dueAt"),it.optBoolean("completed"),it.getLong("createdAt")) }
        require(customers.size <= 100_000 && equipment.size <= 100_000 && jobs.size <= 500_000) { "Backup contains too many records." }
        val customerIds=customers.map { it.id }.toSet(); val equipmentIds=equipment.map { it.id }.toSet()
        require(equipment.all { it.customerId in customerIds } && jobs.all { it.equipmentId in equipmentIds } && reminders.all { it.equipmentId in equipmentIds }) { "Backup relationships are damaged." }
        return BackupPayload(customers,equipment,jobs,templates,reminders)
    }
}
