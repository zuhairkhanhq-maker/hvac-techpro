package com.example.report

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.Customer
import com.example.data.model.Equipment
import com.example.data.model.ServiceJob
import java.io.File
import java.text.DateFormat
import java.util.Date

object ServiceReportWriter {
    fun create(context: Context, customer: Customer, equipment: Equipment, job: ServiceJob, companyName: String = "HVAC TechPro Service"): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "service-job-${job.id}.pdf")
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, 1).create())
        val canvas = page.canvas
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; isFakeBoldText = true }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; isFakeBoldText = true }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        var y = 42f
        canvas.drawText(companyName.take(60), 42f, y, title); y += 28f
        canvas.drawText("Service report #${job.id}", 42f, y, heading); y += 20f
        fun line(label: String, value: String) { if (value.isBlank()) return; canvas.drawText("$label: ${value.take(82)}", 42f, y, body); y += 16f }
        line("Date", DateFormat.getDateTimeInstance().format(Date(job.startedAt)))
        line("Customer", customer.name); line("Phone", customer.phone); line("Address", customer.address)
        y += 8f; canvas.drawText("Equipment", 42f, y, heading); y += 18f
        line("Unit", equipment.nickname); line("Type", equipment.equipmentType); line("Manufacturer", equipment.manufacturer)
        line("Model", equipment.model); line("Serial", equipment.serial); line("Refrigerant", equipment.refrigerant)
        fun section(label: String, value: String) {
            if (value.isBlank()) return
            y += 9f; canvas.drawText(label, 42f, y, heading); y += 17f
            value.lineSequence().flatMap { it.chunked(88).asSequence() }.take(7).forEach { canvas.drawText(it, 42f, y, body); y += 15f }
        }
        section("Reported complaint", job.complaint); section("Measurements", job.readings)
        section("Confirmed findings", job.findings); section("Work performed", job.workPerformed)
        section("Parts used", job.partsUsed); section("Recommendations", job.recommendations)
        y = maxOf(y + 24f, 700f).coerceAtMost(740f)
        canvas.drawLine(42f, y, 250f, y, body); canvas.drawLine(330f, y, 570f, y, body)
        canvas.drawText("Technician", 42f, y + 16f, body); canvas.drawText("Customer approval", 330f, y + 16f, body)
        canvas.drawText("Created offline · Review all measurements before sharing", 42f, 770f, body)
        document.finishPage(page)
        file.outputStream().use { document.writeTo(it) }; document.close()
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share service report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
