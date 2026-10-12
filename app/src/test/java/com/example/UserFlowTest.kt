package com.example

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserTier
import com.example.ui.HvacViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="xhdpi")
class UserFlowTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private lateinit var vm:HvacViewModel
    @Before fun setup(){ val app=ApplicationProvider.getApplicationContext<Application>(); app.getSharedPreferences("hvac_user_prefs",0).edit().clear().commit(); vm=HvacViewModel(app) }
    private fun launch(){ compose.setContent { MyApplicationTheme { HvacApp(vm) } } }

    @Test fun launchFlowIsActionFocused(){ launch(); compose.onNodeWithTag("launch_home").assertExists(); compose.onNodeWithTag("new_job").assertHasClickAction(); compose.onNodeWithTag("main_equipment").performClick(); compose.onNodeWithTag("add_equipment").assertExists(); compose.onNodeWithTag("main_tools").performClick(); compose.onNodeWithText("Calculators").assertExists(); compose.onNodeWithText("PT reference").assertExists() }

    @Test fun emptyJobCannotBeSaved(){
        launch()
        compose.onNodeWithTag("new_job").performClick()
        compose.onNodeWithTag("new_job_form").assertExists()
        compose.onNodeWithText("Add equipment first from the Equipment tab.").assertExists()
        compose.onNodeWithTag("new_job_form").performScrollToNode(hasTestTag("save_job"))
        compose.onNodeWithTag("save_job").assertIsNotEnabled()
    }

    @Test fun plansExplainPaidValue(){ vm.setTier(UserTier.PLUS_USER); launch(); compose.onNodeWithTag("tier_badge_plus_user").performClick(); compose.onNodeWithText("Unlimited equipment and jobs, nameplate OCR, full history, guided workflows and offline PDF reports.").assertExists() }
}
