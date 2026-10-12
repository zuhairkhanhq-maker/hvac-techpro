package com.example.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.Customer
import com.example.data.model.Equipment
import com.example.data.model.ServiceJob
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PortableExportWriter {
    fun create(context: Context, customers: List<Customer>, equipment: List<Equipment>, jobs: List<ServiceJob>): File {
        val root = JSONObject().put("format", "hvac-techpro-export-v1").put("exportedAt", System.currentTimeMillis())
        root.put("customers", JSONArray().apply { customers.forEach { put(JSONObject().put("id",it.id).put("name",it.name).put("phone",it.phone).put("address",it.address).put("createdAt",it.createdAt)) } })
        root.put("equipment", JSONArray().apply { equipment.forEach { put(JSONObject().put("id",it.id).put("customerId",it.customerId).put("nickname",it.nickname).put("manufacturer",it.manufacturer).put("model",it.model).put("serial",it.serial).put("type",it.equipmentType).put("capacity",it.capacity).put("voltage",it.voltage).put("phase",it.phase).put("refrigerant",it.refrigerant).put("notes",it.privateNotes).put("createdAt",it.createdAt)) } })
        root.put("jobs", JSONArray().apply { jobs.forEach { put(JSONObject().put("id",it.id).put("equipmentId",it.equipmentId).put("complaint",it.complaint).put("workflow",it.workflowKey).put("readings",it.readings).put("findings",it.findings).put("workPerformed",it.workPerformed).put("partsUsed",it.partsUsed).put("recommendations",it.recommendations).put("startedAt",it.startedAt)) } })
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }; return File(dir, "hvac-techpro-export.json").also { it.writeText(root.toString(2)) }
    }
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply { type="application/json"; putExtra(Intent.EXTRA_STREAM,uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(send,"Export HVAC records").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
