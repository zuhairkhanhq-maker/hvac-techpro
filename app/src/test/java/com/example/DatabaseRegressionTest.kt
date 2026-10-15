package com.example
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.TroubleshootingLog
import com.example.data.model.Customer
import com.example.data.model.Equipment
import com.example.data.model.ServiceJob
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class DatabaseRegressionTest {
    private fun log()=TroubleshootingLog(title="Customer note",brand="Carrier / Bryant",equipmentType="AC",symptom="Intermittent",rootCause="Investigating",resolution="Recorded measurements")
    @Test fun concurrentSeedAtomicFavoriteAndPersistence()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>(); val name="crud-${System.nanoTime()}.db"
        fun open()=Room.databaseBuilder(c,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_1_2,AppDatabase.MIGRATION_2_3,AppDatabase.MIGRATION_3_4).build()
        var db=open()
        try {
            val dao=db.troubleshootingDao()
            coroutineScope { repeat(12) { launch(Dispatchers.IO) { dao.seedIfEmpty() } } }; assertEquals(4,dao.getCount())
            val id=dao.insertLog(log())
            coroutineScope { repeat(10) { launch(Dispatchers.IO) { dao.toggleFavorite(id) } } }; assertFalse(dao.getLogById(id)!!.isFavorite)
            dao.toggleFavorite(id); assertTrue(dao.getLogById(id)!!.isFavorite)
            assertTrue(dao.searchLogs("Customer").first().any { it.id==id })
            val preset=dao.getAllLogs().first().first { it.isPresetCheatSheet }; dao.deleteLogById(preset.id); assertNotNull(dao.getLogById(preset.id))
            db.close(); db=open(); assertEquals("Customer note",db.troubleshootingDao().getLogById(id)?.title)
            db.troubleshootingDao().deleteLogById(id); assertNull(db.troubleshootingDao().getLogById(id))
        } finally { db.close(); c.deleteDatabase(name) }
    }
    @Test fun upgradePreservesUserRowsAndRetiresOnlyPresets()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>(); val name="upgrade-${System.nanoTime()}.db"
        fun open()=Room.databaseBuilder(c,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_1_2,AppDatabase.MIGRATION_2_3,AppDatabase.MIGRATION_3_4).build()
        var db=open()
        try {
            db.troubleshootingDao().seedIfEmpty(); val id=db.troubleshootingDao().insertLog(log()); db.close()
            android.database.sqlite.SQLiteDatabase.openDatabase(c.getDatabasePath(name).path,null,0).use { it.version=1 }
            db=open(); assertEquals(1,db.troubleshootingDao().getCount()); assertNotNull(db.troubleshootingDao().getLogById(id))
            db.troubleshootingDao().seedIfEmpty(); assertEquals(5,db.troubleshootingDao().getCount())
        } finally { db.close(); c.deleteDatabase(name) }
    }
    @Test fun fieldRecordsPersistWithRelationships()=runBlocking {
        val c=ApplicationProvider.getApplicationContext<Context>(); val name="field-${System.nanoTime()}.db"
        val db=Room.databaseBuilder(c,AppDatabase::class.java,name).addMigrations(AppDatabase.MIGRATION_1_2,AppDatabase.MIGRATION_2_3,AppDatabase.MIGRATION_3_4).build()
        try { val dao=db.fieldDao(); val customer=dao.insertCustomer(Customer(name="Test customer")); val unit=dao.insertEquipment(Equipment(customerId=customer,nickname="Roof unit",model="ABC123")); dao.insertJob(ServiceJob(equipmentId=unit,complaint="No cooling",findings="Restricted filter",workPerformed="Replaced filter")); assertEquals(1,dao.equipmentCount()); assertEquals(1,dao.jobCount()); assertEquals("ABC123",dao.equipmentById(unit)?.model) } finally { db.close(); c.deleteDatabase(name) }
    }
}
