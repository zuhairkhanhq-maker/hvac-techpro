package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.Customer
import com.example.data.model.Equipment
import com.example.data.model.EquipmentWithCustomer
import com.example.data.model.ServiceJob
import com.example.data.model.InspectionTemplate
import com.example.data.model.MaintenanceReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE") fun customers(): Flow<List<Customer>>
    @Query("SELECT * FROM equipment ORDER BY createdAt DESC") fun equipment(): Flow<List<Equipment>>
    @Query("SELECT * FROM service_jobs ORDER BY startedAt DESC") fun jobs(): Flow<List<ServiceJob>>
    @Query("SELECT * FROM inspection_templates ORDER BY builtIn DESC, name COLLATE NOCASE") fun templates(): Flow<List<InspectionTemplate>>
    @Query("SELECT * FROM maintenance_reminders ORDER BY completed, dueAt") fun reminders(): Flow<List<MaintenanceReminder>>

    @Query("""SELECT e.id, e.customerId, e.nickname, e.manufacturer, e.model, e.serial,
        e.equipmentType, e.refrigerant, c.name AS customerName,
        (SELECT COUNT(*) FROM service_jobs j WHERE j.equipmentId=e.id) AS jobCount,
        (SELECT MAX(startedAt) FROM service_jobs j WHERE j.equipmentId=e.id) AS lastServiceAt
        FROM equipment e JOIN customers c ON c.id=e.customerId
        ORDER BY COALESCE(lastServiceAt, e.createdAt) DESC""")
    fun equipmentOverview(): Flow<List<EquipmentWithCustomer>>

    @Query("SELECT * FROM service_jobs WHERE equipmentId=:equipmentId ORDER BY startedAt DESC")
    fun jobsForEquipment(equipmentId: Long): Flow<List<ServiceJob>>

    @Query("SELECT * FROM equipment WHERE id=:id") suspend fun equipmentById(id: Long): Equipment?
    @Query("SELECT * FROM customers WHERE id=:id") suspend fun customerById(id: Long): Customer?
    @Query("SELECT * FROM service_jobs WHERE id=:id") suspend fun jobById(id: Long): ServiceJob?
    @Query("SELECT COUNT(*) FROM equipment") suspend fun equipmentCount(): Int
    @Query("SELECT COUNT(*) FROM service_jobs") suspend fun jobCount(): Int
    @Query("SELECT * FROM customers ORDER BY id") suspend fun allCustomers(): List<Customer>
    @Query("SELECT * FROM equipment ORDER BY id") suspend fun allEquipment(): List<Equipment>
    @Query("SELECT * FROM service_jobs ORDER BY id") suspend fun allJobs(): List<ServiceJob>
    @Query("SELECT * FROM inspection_templates ORDER BY id") suspend fun allTemplates(): List<InspectionTemplate>
    @Query("SELECT * FROM maintenance_reminders ORDER BY id") suspend fun allReminders(): List<MaintenanceReminder>

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCustomer(customer: Customer): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEquipment(equipment: Equipment): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertJob(job: ServiceJob): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTemplate(template: InspectionTemplate): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReminder(reminder: MaintenanceReminder): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCustomers(items: List<Customer>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEquipment(items: List<Equipment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertJobs(items: List<ServiceJob>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTemplates(items: List<InspectionTemplate>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReminders(items: List<MaintenanceReminder>)

    @Transaction
    suspend fun insertCustomerAndEquipment(customer: Customer, equipment: Equipment): Long {
        val customerId = insertCustomer(customer)
        return insertEquipment(equipment.copy(customerId = customerId))
    }

    @Query("DELETE FROM customers WHERE id=:id") suspend fun deleteCustomer(id: Long)
    @Query("DELETE FROM equipment WHERE id=:id") suspend fun deleteEquipment(id: Long)
    @Query("DELETE FROM service_jobs WHERE id=:id") suspend fun deleteJob(id: Long)
    @Query("DELETE FROM inspection_templates WHERE id=:id AND builtIn=0") suspend fun deleteTemplate(id: Long)
    @Query("UPDATE maintenance_reminders SET completed=:completed WHERE id=:id") suspend fun setReminderCompleted(id: Long, completed: Boolean)
    @Query("DELETE FROM maintenance_reminders WHERE id=:id") suspend fun deleteReminder(id: Long)
    @Query("DELETE FROM service_jobs") suspend fun clearJobs()
    @Query("DELETE FROM equipment") suspend fun clearEquipment()
    @Query("DELETE FROM customers") suspend fun clearCustomers()
    @Query("DELETE FROM inspection_templates") suspend fun clearTemplates()
    @Query("DELETE FROM maintenance_reminders") suspend fun clearReminders()

    @Transaction
    suspend fun replaceAll(customers: List<Customer>, equipment: List<Equipment>, jobs: List<ServiceJob>, templates: List<InspectionTemplate>, reminders: List<MaintenanceReminder>) {
        clearReminders(); clearJobs(); clearEquipment(); clearCustomers(); clearTemplates()
        insertCustomers(customers); insertEquipment(equipment); insertJobs(jobs); insertTemplates(templates); insertReminders(reminders)
    }
}
