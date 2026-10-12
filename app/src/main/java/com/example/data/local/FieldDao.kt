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
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE") fun customers(): Flow<List<Customer>>
    @Query("SELECT * FROM equipment ORDER BY createdAt DESC") fun equipment(): Flow<List<Equipment>>
    @Query("SELECT * FROM service_jobs ORDER BY startedAt DESC") fun jobs(): Flow<List<ServiceJob>>

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

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCustomer(customer: Customer): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEquipment(equipment: Equipment): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertJob(job: ServiceJob): Long

    @Transaction
    suspend fun insertCustomerAndEquipment(customer: Customer, equipment: Equipment): Long {
        val customerId = insertCustomer(customer)
        return insertEquipment(equipment.copy(customerId = customerId))
    }

    @Query("DELETE FROM customers WHERE id=:id") suspend fun deleteCustomer(id: Long)
    @Query("DELETE FROM equipment WHERE id=:id") suspend fun deleteEquipment(id: Long)
    @Query("DELETE FROM service_jobs WHERE id=:id") suspend fun deleteJob(id: Long)
}
