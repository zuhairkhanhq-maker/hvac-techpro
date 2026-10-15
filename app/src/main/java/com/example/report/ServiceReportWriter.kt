package com.example.report

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Customer
import com.example.data.model.Equipment
import com.example.data.model.ServiceJob
import java.io.File
import java.text.DateFormat
import java.util.Date

data class ReportIdentity(val companyName: String, val contact: String = "", val logoPath: String = "")

object ServiceReportWriter {
    fun create(context: Context, customer: Customer, equipment: Equipment, job: ServiceJob, identity: ReportIdentity = ReportIdentity("HVAC TechPro Service")): File {
        val dir=File(context.cacheDir,"reports").apply{mkdirs()}; val file=File(dir,"service-job-${job.id}.pdf")
        val document=PdfDocument(); val title=Paint(1).apply{textSize=20f;isFakeBoldText=true}; val heading=Paint(1).apply{textSize=13f;isFakeBoldText=true}; val body=Paint(1).apply{textSize=10.5f}
        val page=document.startPage(PdfDocument.PageInfo.Builder(612,792,1).create()); val c=page.canvas; var y=42f
        loadBitmap(context,identity.logoPath)?.let { bmp -> c.drawBitmap(bmp,null,RectF(500f,24f,570f,84f),body) }
        c.drawText(identity.companyName.take(60),42f,y,title); y+=21f
        if(identity.contact.isNotBlank()){ identity.contact.lineSequence().take(2).forEach{c.drawText(it.take(84),42f,y,body);y+=14f} }
        y+=8f; c.drawText("Service report #${job.id}",42f,y,heading); y+=18f
        fun line(label:String,value:String){if(value.isBlank())return;c.drawText("$label: ${value.take(82)}",42f,y,body);y+=14f}
        line("Date",DateFormat.getDateTimeInstance().format(Date(job.startedAt))); line("Customer",customer.name);line("Phone",customer.phone);line("Address",customer.address)
        y+=6f;c.drawText("Equipment",42f,y,heading);y+=16f
        line("Unit",equipment.nickname);line("Type",equipment.equipmentType);line("Manufacturer",equipment.manufacturer);line("Model",equipment.model);line("Serial",equipment.serial);line("Refrigerant",equipment.refrigerant)
        fun section(label:String,value:String,max:Int=5){if(value.isBlank())return;y+=7f;c.drawText(label,42f,y,heading);y+=15f;value.lineSequence().flatMap{it.chunked(92).asSequence()}.take(max).forEach{c.drawText(it,42f,y,body);y+=13f}}
        section("Reported complaint",job.complaint,3);section("Measurements",job.readings,4);section("Confirmed findings",job.findings,4);section("Work performed",job.workPerformed,4);section("Parts used",job.partsUsed,2);section("Recommendations",job.recommendations,3)
        section("Inspection · ${job.templateName}",job.checklistResults,4)
        val signatureY=maxOf(y+20f,680f).coerceAtMost(720f); c.drawLine(42f,signatureY,260f,signatureY,body);c.drawLine(330f,signatureY,570f,signatureY,body)
        c.drawText("Technician: ${job.technicianSignature.ifBlank{"Not signed"}.take(28)}",42f,signatureY+16f,body);c.drawText("Customer: ${job.customerSignature.ifBlank{"Not signed"}.take(28)}",330f,signatureY+16f,body)
        if(job.reportTerms.isNotBlank())c.drawText(job.reportTerms.replace('\n',' ').take(100),42f,755f,body)
        c.drawText("Created locally on this device",42f,773f,body);document.finishPage(page)
        if(job.beforePhotoUri.isNotBlank()||job.afterPhotoUri.isNotBlank()){
            val p2=document.startPage(PdfDocument.PageInfo.Builder(612,792,2).create());val pc=p2.canvas;pc.drawText("Job photographs",42f,42f,title)
            listOf("Before" to job.beforePhotoUri,"After" to job.afterPhotoUri).forEachIndexed{i,(label,path)-> val top=80f+i*340f;pc.drawText(label,42f,top,heading);loadBitmap(context,path)?.let{pc.drawBitmap(it,null,RectF(42f,top+16f,570f,top+316f),body)}?:pc.drawText("Photograph unavailable",42f,top+40f,body)}
            document.finishPage(p2)
        }
        file.outputStream().use{document.writeTo(it)};document.close();return file
    }
    private fun loadBitmap(context:Context,path:String)=runCatching{if(path.isBlank())null else context.contentResolver.openInputStream(Uri.parse(path))?.use(BitmapFactory::decodeStream)}.getOrNull()
    fun share(context:Context,file:File){val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file);val intent=Intent(Intent.ACTION_SEND).apply{type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)};context.startActivity(Intent.createChooser(intent,"Share service report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
}
