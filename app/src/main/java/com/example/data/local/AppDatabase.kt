package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.TroubleshootingLog
import com.example.data.model.Customer
import com.example.data.model.Equipment
import com.example.data.model.ServiceJob
import com.example.data.model.InspectionTemplate
import com.example.data.model.MaintenanceReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TroubleshootingLog::class, Customer::class, Equipment::class, ServiceJob::class, InspectionTemplate::class, MaintenanceReminder::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun troubleshootingDao(): TroubleshootingDao
    abstract fun fieldDao(): FieldDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hvac_field_database.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM troubleshooting_logs WHERE isPresetCheatSheet = 1")
            }
        }
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_name` ON `customers` (`name`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `equipment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `nickname` TEXT NOT NULL, `manufacturer` TEXT NOT NULL, `model` TEXT NOT NULL, `serial` TEXT NOT NULL, `equipmentType` TEXT NOT NULL, `capacity` TEXT NOT NULL, `voltage` TEXT NOT NULL, `phase` TEXT NOT NULL, `refrigerant` TEXT NOT NULL, `meteringDevice` TEXT NOT NULL, `installedOn` TEXT NOT NULL, `nameplateUri` TEXT NOT NULL, `privateNotes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_customerId` ON `equipment` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_manufacturer` ON `equipment` (`manufacturer`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_model` ON `equipment` (`model`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_serial` ON `equipment` (`serial`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `service_jobs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `equipmentId` INTEGER NOT NULL, `complaint` TEXT NOT NULL, `workflowKey` TEXT NOT NULL, `readings` TEXT NOT NULL, `findings` TEXT NOT NULL, `workPerformed` TEXT NOT NULL, `partsUsed` TEXT NOT NULL, `recommendations` TEXT NOT NULL, `status` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `completedAt` INTEGER, FOREIGN KEY(`equipmentId`) REFERENCES `equipment`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_jobs_equipmentId` ON `service_jobs` (`equipmentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_jobs_status` ON `service_jobs` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_jobs_startedAt` ON `service_jobs` (`startedAt`)")
            }
        }
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val existing = mutableSetOf<String>()
                db.query("PRAGMA table_info(`service_jobs`)").use { cursor ->
                    val index=cursor.getColumnIndex("name")
                    while(cursor.moveToNext()) if(index>=0) existing += cursor.getString(index)
                }
                listOf("templateName", "checklistResults", "beforePhotoUri", "afterPhotoUri", "technicianSignature", "customerSignature", "reportTerms").forEach {
                    if(it !in existing) db.execSQL("ALTER TABLE `service_jobs` ADD COLUMN `$it` TEXT NOT NULL DEFAULT ''")
                }
                db.execSQL("CREATE TABLE IF NOT EXISTS `inspection_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `items` TEXT NOT NULL, `builtIn` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inspection_templates_name` ON `inspection_templates` (`name`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `maintenance_reminders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `equipmentId` INTEGER NOT NULL, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `dueAt` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`equipmentId`) REFERENCES `equipment`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_reminders_equipmentId` ON `maintenance_reminders` (`equipmentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_reminders_dueAt` ON `maintenance_reminders` (`dueAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_reminders_completed` ON `maintenance_reminders` (`completed`)")
            }
        }
        val INITIAL_CHEAT_SHEETS = listOf(
            reference("Record superheat and subcooling", "Record airflow, pressures, temperatures and operating conditions.", "Compare with the exact equipment charging chart; blends require dew and bubble data."),
            reference("Record a compressor start issue", "Record the model, fault and starting symptom.", "Do not repeatedly reset protection. Qualified technicians must use the manufacturer service procedure."),
            reference("Record a furnace lockout", "Record the fault pattern and full model before resetting.", "Do not bypass safety controls. Follow the exact equipment instructions."),
            reference("Write a useful service note", "Record measurements, model, observations and work performed.", "Distinguish suspected causes from confirmed causes and document verification.")
        )
        private fun reference(title: String, symptom: String, note: String) = TroubleshootingLog(
            title = title, brand = "Universal", equipmentType = "Reference", symptom = symptom,
            rootCause = "No equipment diagnosed", resolution = note, isPresetCheatSheet = true)
    }
}
