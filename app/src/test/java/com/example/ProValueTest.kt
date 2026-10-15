package com.example

import com.example.data.model.*
import com.example.report.BackupPayload
import com.example.report.EncryptedBackupManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ProValueTest {
    private fun payload(): BackupPayload {
        val customer=Customer(id=1,name="Northside Market")
        val unit=Equipment(id=2,customerId=1,nickname="Roof unit")
        val job=ServiceJob(id=3,equipmentId=2,complaint="No cooling",findings="Failed capacitor",workPerformed="Replaced and verified",templateName="Repair verification",checklistResults="[x] Verify operation",technicianSignature="Alex Tech",customerSignature="Sam Owner",reportTerms="90 day labor warranty")
        val template=InspectionTemplate(id=4,name="Repair verification",category="Repair",items="Record complaint\nVerify operation")
        val reminder=MaintenanceReminder(id=5,equipmentId=2,title="Fall maintenance",dueAt=2_000_000)
        return BackupPayload(listOf(customer),listOf(unit),listOf(job),listOf(template),listOf(reminder))
    }

    @Test fun encryptedBackupRoundTripsAllProRecords() {
        val bytes=EncryptedBackupManager.encrypt(payload(),"correct horse".toCharArray())
        val restored=EncryptedBackupManager.decrypt(bytes,"correct horse".toCharArray())
        assertEquals("Sam Owner",restored.jobs.single().customerSignature)
        assertEquals("Repair verification",restored.templates.single().name)
        assertEquals("Fall maintenance",restored.reminders.single().title)
        assertFalse(bytes.toString(Charsets.UTF_8).contains("Northside Market"))
    }

    @Test fun wrongPasswordAndBrokenRelationshipsAreRejected() {
        val bytes=EncryptedBackupManager.encrypt(payload(),"correct horse".toCharArray())
        assertThrows(Exception::class.java){EncryptedBackupManager.decrypt(bytes,"wrong password".toCharArray())}
        val invalid=payload().copy(equipment=listOf(payload().equipment.single().copy(customerId=999)))
        val encrypted=EncryptedBackupManager.encrypt(invalid,"correct horse".toCharArray())
        assertThrows(IllegalArgumentException::class.java){EncryptedBackupManager.decrypt(encrypted,"correct horse".toCharArray())}
    }

    @Test fun proTierContainsRecurringBusinessCapabilities() {
        with(UserTier.PRO_USER){
            assertTrue(canUseBranding);assertTrue(canUseInspectionTemplates);assertTrue(canUseMaintenanceReminders)
            assertTrue(canUseEncryptedBackup);assertTrue(canUseBusinessDashboard);assertEquals(Int.MAX_VALUE,jobLimit)
        }
        assertFalse(UserTier.PLUS_USER.canUseEncryptedBackup)
    }
}
