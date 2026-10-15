package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.report.LocalMediaStore
import com.example.ui.HvacViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProWorkspaceScreen(viewModel: HvacViewModel) {
    val tier by viewModel.currentTier.collectAsState(); if(!tier.canUseBusinessDashboard){Box(Modifier.fillMaxSize().padding(24.dp)){Text("Pro workspace requires the Pro plan.")};return}
    val context=LocalContext.current;val scope=rememberCoroutineScope();val jobs by viewModel.serviceJobs.collectAsState();val equipment by viewModel.equipmentOverview.collectAsState();val templates by viewModel.inspectionTemplates.collectAsState();val reminders by viewModel.maintenanceReminders.collectAsState()
    val company by viewModel.companyName.collectAsState();val contact by viewModel.companyContact.collectAsState();val terms by viewModel.reportTerms.collectAsState();val logo by viewModel.companyLogoPath.collectAsState()
    var companyDraft by remember(company){mutableStateOf(company)};var contactDraft by remember(contact){mutableStateOf(contact)};var termsDraft by remember(terms){mutableStateOf(terms)}
    var templateName by rememberSaveable{mutableStateOf("")};var templateCategory by rememberSaveable{mutableStateOf("Custom")};var templateItems by rememberSaveable{mutableStateOf("")}
    var reminderUnit by rememberSaveable{mutableLongStateOf(0)};var reminderTitle by rememberSaveable{mutableStateOf("")};var reminderNotes by rememberSaveable{mutableStateOf("")};var reminderDate by rememberSaveable{mutableStateOf("")};var unitMenu by remember{mutableStateOf(false)}
    var password by rememberSaveable{mutableStateOf("")};var backupStatus by rememberSaveable{mutableStateOf<String?>(null)}
    val logoPicker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->uri?.let{runCatching{LocalMediaStore.import(context,it,"company_logo_")}.onSuccess{path->viewModel.setBusinessIdentity(companyDraft,contactDraft,termsDraft,path)}.onFailure{backupStatus="Logo could not be imported."}}}
    val createBackup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->if(uri!=null)scope.launch{runCatching{viewModel.createEncryptedBackup(password.toCharArray())}.onSuccess{bytes->withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri,"w")!!.use{it.write(bytes)}};backupStatus="Encrypted backup saved."}.onFailure{backupStatus=it.message?:"Backup failed."}}}
    val openBackup=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{runCatching{withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.use{it.readBytes()}}}.mapCatching{viewModel.restoreEncryptedBackup(it,password.toCharArray())}.onSuccess{backupStatus="Backup verified and restored."}.onFailure{backupStatus="Restore rejected: wrong password or damaged backup."}}}
    val now=System.currentTimeMillis();val monthStart=now-30L*24*60*60*1000;val monthJobs=jobs.count{it.startedAt>=monthStart};val overdue=reminders.count{!it.completed&&it.dueAt<now};val parts=jobs.count{it.partsUsed.isNotBlank()};val signed=jobs.count{it.customerSignature.isNotBlank()}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp,16.dp,16.dp,96.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("Pro workspace",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Business tools calculated and stored on this device.")}
        item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text("Last 30 days",fontWeight=FontWeight.Bold);Text("$monthJobs jobs · $parts with parts · $signed customer approvals");Text("$overdue overdue · ${reminders.count{!it.completed&&it.dueAt>=now}} upcoming reminders");Text("${equipment.size} tracked units · ${jobs.size} lifetime jobs",style=MaterialTheme.typography.bodySmall)}}}
        item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Report identity",fontWeight=FontWeight.Bold);ProField("Company name",companyDraft){companyDraft=it};ProField("Phone · email · license",contactDraft){contactDraft=it};OutlinedTextField(termsDraft,{termsDraft=it},Modifier.fillMaxWidth(),label={Text("Terms and warranty text")},minLines=3);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({logoPicker.launch("image/*")}){Text(if(logo.isBlank())"Add logo" else "Replace logo")};Button({viewModel.setBusinessIdentity(companyDraft,contactDraft,termsDraft)}){Text("Save identity")}}}}}
        item{Text("Inspection templates",style=MaterialTheme.typography.titleLarge)}
        items(templates,key={it.id}){t->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(t.name,fontWeight=FontWeight.Bold);Text("${t.category} · ${t.items.lineSequence().count()} checks");if(!t.builtIn)TextButton({viewModel.deleteInspectionTemplate(t.id)}){Text("Delete")}}}}
        item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Create reusable checklist",fontWeight=FontWeight.Bold);ProField("Template name",templateName){templateName=it};ProField("Category",templateCategory){templateCategory=it};OutlinedTextField(templateItems,{templateItems=it},Modifier.fillMaxWidth(),label={Text("One checklist item per line")},minLines=4);Button({viewModel.addInspectionTemplate(templateName,templateCategory,templateItems);templateName="";templateItems=""},Modifier.fillMaxWidth()){Text("Save template")}}}}
        item{Text("Maintenance reminders",style=MaterialTheme.typography.titleLarge)}
        items(reminders,key={it.id}){r->val unit=equipment.firstOrNull{it.id==r.equipmentId};Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){Checkbox(r.completed,{viewModel.setReminderCompleted(r.id,it)});Column{Text(r.title,fontWeight=FontWeight.Bold);Text("${unit?.nickname?:"Unit"} · ${DateFormat.getDateInstance().format(Date(r.dueAt))}");if(r.notes.isNotBlank())Text(r.notes,style=MaterialTheme.typography.bodySmall)}}}}
        item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Schedule follow-up",fontWeight=FontWeight.Bold);Box{OutlinedButton({unitMenu=true},Modifier.fillMaxWidth()){Text(equipment.firstOrNull{it.id==reminderUnit}?.nickname?:"Choose equipment")};DropdownMenu(unitMenu,{unitMenu=false}){equipment.forEach{u->DropdownMenuItem({Text("${u.customerName} · ${u.nickname}")},{reminderUnit=u.id;unitMenu=false})}}};ProField("Reminder",reminderTitle){reminderTitle=it};ProField("Due date · YYYY-MM-DD",reminderDate){reminderDate=it};ProField("Notes",reminderNotes){reminderNotes=it};Button({val due=runCatching{SimpleDateFormat("yyyy-MM-dd",Locale.US).apply{isLenient=false}.parse(reminderDate)?.time?:0}.getOrDefault(0);viewModel.addMaintenanceReminder(reminderUnit,reminderTitle,reminderNotes,due)},Modifier.fillMaxWidth()){Text("Add reminder")}}}}
        item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Encrypted recovery",fontWeight=FontWeight.Bold);Text("The backup is encrypted before it leaves this device. Keep the password safe; it cannot be recovered.");OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text("Backup password · 8+ characters")},visualTransformation=PasswordVisualTransformation(),singleLine=true);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({createBackup.launch("hvac-techpro-${System.currentTimeMillis()}.hvacbackup")},enabled=password.length>=8,modifier=Modifier.weight(1f)){Text("Create backup")};OutlinedButton({openBackup.launch(arrayOf("application/octet-stream","application/json","*/*"))},enabled=password.length>=8,modifier=Modifier.weight(1f)){Text("Restore")}};backupStatus?.let{Text(it,style=MaterialTheme.typography.bodySmall)}}}}
        item{Text("Photos, signatures, templates, reminders, reports and backups remain local. A file leaves the app only when you choose a destination.",style=MaterialTheme.typography.bodySmall)}
    }
}

@Composable private fun ProField(label:String,value:String,onChange:(String)->Unit){OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),label={Text(label)},singleLine=true)}
