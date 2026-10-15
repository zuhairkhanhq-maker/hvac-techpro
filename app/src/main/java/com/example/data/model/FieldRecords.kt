package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "customers", indices = [Index("name")])
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "equipment",
    foreignKeys = [ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("customerId"), Index("manufacturer"), Index("model"), Index(value = ["serial"], unique = false)]
)
data class Equipment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val nickname: String,
    val manufacturer: String = "",
    val model: String = "",
    val serial: String = "",
    val equipmentType: String = "Split system",
    val capacity: String = "",
    val voltage: String = "",
    val phase: String = "",
    val refrigerant: String = "",
    val meteringDevice: String = "",
    val installedOn: String = "",
    val nameplateUri: String = "",
    val privateNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "service_jobs",
    foreignKeys = [ForeignKey(entity = Equipment::class, parentColumns = ["id"], childColumns = ["equipmentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("equipmentId"), Index("status"), Index("startedAt")]
)
data class ServiceJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipmentId: Long,
    val complaint: String,
    val workflowKey: String = "general",
    val readings: String = "",
    val findings: String = "",
    val workPerformed: String = "",
    val partsUsed: String = "",
    val recommendations: String = "",
    val templateName: String = "",
    val checklistResults: String = "",
    val beforePhotoUri: String = "",
    val afterPhotoUri: String = "",
    val technicianSignature: String = "",
    val customerSignature: String = "",
    val reportTerms: String = "",
    val status: String = "COMPLETE",
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = System.currentTimeMillis()
)

@Entity(tableName = "inspection_templates", indices = [Index("name")])
data class InspectionTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val items: String,
    val builtIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "maintenance_reminders",
    foreignKeys = [ForeignKey(entity = Equipment::class, parentColumns = ["id"], childColumns = ["equipmentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("equipmentId"), Index("dueAt"), Index("completed")]
)
data class MaintenanceReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipmentId: Long,
    val title: String,
    val notes: String = "",
    val dueAt: Long,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class EquipmentWithCustomer(
    val id: Long,
    val customerId: Long,
    val nickname: String,
    val manufacturer: String,
    val model: String,
    val serial: String,
    val equipmentType: String,
    val refrigerant: String,
    val customerName: String,
    val jobCount: Int,
    val lastServiceAt: Long?
)

data class NameplateFields(
    val manufacturer: String = "",
    val model: String = "",
    val serial: String = "",
    val voltage: String = "",
    val phase: String = "",
    val refrigerant: String = "",
    val capacity: String = "",
    val confidence: Int = 0
)
