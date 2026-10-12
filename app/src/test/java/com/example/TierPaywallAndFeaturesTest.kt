package com.example
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserSessionManager
import com.example.data.model.*
import com.example.data.remote.SeniorTechAiService
import com.example.ui.HvacViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class TierPaywallAndFeaturesTest {
    private lateinit var app: Application
    private lateinit var vm: HvacViewModel
    @Before fun setup() {
        app=ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("hvac_user_prefs",0).edit().clear().commit()
        vm=HvacViewModel(app)
    }
    @Test fun releaseRejectsLocalPaidEntitlements() {
        UserSessionManager(app,allowTierPreview=true).setTier(UserTier.PRO_USER)
        val release=UserSessionManager(app,allowTierPreview=false)
        assertEquals(UserTier.REGULAR_USER,release.currentTier.value)
        release.setTier(UserTier.PLUS_USER); assertEquals(UserTier.REGULAR_USER,release.currentTier.value)
    }
    @Test fun quotaPersistsAndResetsOnlyAtBoundary() {
        app.getSharedPreferences("hvac_user_prefs",0).edit().clear().commit()
        var time=100000L
        val s=UserSessionManager(app,now={time},allowTierPreview=true)
        repeat(3) { assertTrue(s.tryConsumeVideoScan()) }; assertFalse(s.tryConsumeVideoScan())
        assertFalse(UserSessionManager(app,now={time}).tryConsumeVideoScan())
        time-=100; assertFalse(s.tryConsumeVideoScan())
        time=100000L+UserSessionManager.WEEK_MS; assertTrue(s.tryConsumeVideoScan()); assertEquals(1,s.weeklyVideoScansUsed.value)
    }
    @Test fun plusAndUnlimitedProQuota() {
        val s=UserSessionManager(app,allowTierPreview=true); s.setTier(UserTier.PLUS_USER)
        repeat(15) { assertTrue(s.tryConsumeVideoScan()) }; assertFalse(s.tryConsumeVideoScan())
        s.setTier(UserTier.PRO_USER); repeat(1200) { assertTrue(s.tryConsumeVideoScan()) }
        s.setTier(UserTier.REGULAR_USER); assertFalse(s.tryConsumeVideoScan())
    }
    @Test fun onboardingPersists() {
        UserSessionManager(app).setOnboardingCompleted(true)
        val s=UserSessionManager(app); assertTrue(s.onboardingCompleted.value); assertEquals(UserTier.REGULAR_USER,s.currentTier.value)
    }
    @Test fun freeCannotBypassPaidOperations() {
        val before=vm.diagnosticMessages.value
        vm.sendDiagnosticQuery("compressor trip"); vm.runSchematicScan(null)
        assertEquals(before,vm.diagnosticMessages.value); assertFalse(vm.isScanning.value); assertEquals(0,vm.aiQueriesUsed.value)
    }
    @Test fun examplesDoNotConsumeQuota() {
        repeat(10) { vm.runVideoDiagnostic(VideoDiagnosticDatabase.samples.first()) }
        assertEquals(0,vm.weeklyVideoScansUsed.value); assertNotNull(vm.videoScanResult.value)
    }
    @Test fun airflowArithmeticAndZeroDelta() {
        assertEquals(1667,vm.calculateCfm().cfm)
        vm.cfmMode.value="Cooling Tonnage"; assertEquals(1200,vm.calculateCfm().cfm)
        vm.cfmMode.value="Electric Strip"; vm.cfmVolts.value="240"; vm.cfmAmps.value="40"; vm.cfmDeltaT.value="30"
        assertEquals(1011,vm.calculateCfm().cfm)
        vm.cfmDeltaT.value="0"; assertThrows(IllegalArgumentException::class.java) { vm.calculateCfm() }
    }
    @Test fun invalidMeasurementsNeverUseDefaults() {
        listOf("","bad","NaN","Infinity","1e999").forEach { value ->
            vm.shSuctionPressure.value=value; assertThrows(IllegalArgumentException::class.java) { vm.calculateSuperheat() }
            vm.scLiquidTemp.value=value; assertThrows(IllegalArgumentException::class.java) { vm.calculateSubcooling() }
            vm.ductCfm.value=value; assertThrows(IllegalArgumentException::class.java) { vm.calculateDuct() }
        }
    }
    @Test fun higherGlideSubcoolingRequiresBubbleTable() {
        vm.selectedScRefrigerant.value=RefrigerantData.R454B
        assertThrows(IllegalArgumentException::class.java) { vm.calculateSubcooling() }
    }
    @Test fun targetFormulaRejectsOutOfRangeWeather() {
        vm.shIndoorWetBulb.value="30"; vm.shOutdoorDryBulb.value="140"
        assertThrows(IllegalArgumentException::class.java) { vm.calculateSuperheat() }
    }
    @Test fun incompleteLogIsNotSaved() {
        vm.addTroubleshootingLog("","","","","","","","")
        assertNotNull(vm.operationError.value); assertFalse(vm.savingLog.value)
    }
    @Test fun guideAndScannerDoNotInventResults() = runBlocking {
        val service=SeniorTechAiService()
        assertTrue(service.diagnoseFieldIssue("Compressor start issue").contains("Compressor starting"))
        val reply=service.diagnoseFieldIssue("compressor buzzing")
        assertTrue(reply.contains("not an AI diagnosis")); assertFalse(reply.contains("75%"))
        assertTrue(service.diagnoseFieldIssue("gas smell at furnace").contains("leave the area"))
        try { service.scanSchematicOrDataPlate(null,"Carrier"); fail("Preset must not produce OCR") } catch (_:IllegalArgumentException) {}
    }
}
