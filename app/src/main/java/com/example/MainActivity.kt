package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.HvacViewModel
import com.example.ui.components.TierBadge
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HvacViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent { MyApplicationTheme { HvacApp(viewModel) } }
    }
}

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home), EQUIPMENT("Equipment", Icons.Default.AcUnit),
    TOOLS("Field tools", Icons.Default.Calculate), REFERENCE("Reference", Icons.Default.MenuBook),
    SETTINGS("Settings", Icons.Default.Settings)
}
private enum class Route { MAIN, NEW_JOB, ERROR_CODES, GUIDE, A2L }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HvacApp(viewModel: HvacViewModel) {
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var route by rememberSaveable { mutableStateOf(Route.MAIN) }
    var showPlans by rememberSaveable { mutableStateOf(false) }
    val tier by viewModel.currentTier.collectAsState(); val error by viewModel.operationError.collectAsState()
    if (error != null) AlertDialog(onDismissRequest = viewModel::clearOperationError, title = { Text("Action needs attention") }, text = { Text(error.orEmpty()) }, confirmButton = { TextButton(onClick = viewModel::clearOperationError) { Text("OK") } })
    BackHandler(route != Route.MAIN) { route = Route.MAIN }
    Scaffold(
        topBar = { TopAppBar(title = { Text(when (route) { Route.NEW_JOB -> "New job"; Route.ERROR_CODES -> "Fault reference"; Route.GUIDE -> "Troubleshooting guide"; Route.A2L -> "A2L worksheet"; else -> "HVAC TechPro" }) }, navigationIcon = { if (route != Route.MAIN) IconButton(onClick = { route = Route.MAIN }) { Icon(Icons.Default.ArrowBack, "Back") } }, actions = { TierBadge(tier = tier, modifier = Modifier.padding(end = 6.dp), onClick = { showPlans = true }) }) },
        bottomBar = { if (route == Route.MAIN) NavigationBar { MainTab.entries.forEach { item -> NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, maxLines = 1) }, modifier = Modifier.testTag("main_${item.name.lowercase()}")) } } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (route) {
                Route.NEW_JOB -> NewServiceJobScreen(viewModel) { route = Route.MAIN; tab = MainTab.HOME }
                Route.ERROR_CODES -> ErrorCodesScreen(viewModel) { showPlans = true }
                Route.GUIDE -> SeniorTechScreen(viewModel) { showPlans = true }
                Route.A2L -> A2LComplianceScreen(viewModel) { showPlans = true }
                Route.MAIN -> when (tab) {
                    MainTab.HOME -> LaunchHomeScreen(viewModel, { route = Route.NEW_JOB }, { tab = MainTab.EQUIPMENT }, { showPlans = true })
                    MainTab.EQUIPMENT -> EquipmentRecordsScreen(viewModel)
                    MainTab.TOOLS -> FieldToolsScreen(viewModel)
                    MainTab.REFERENCE -> ReferenceHubScreen(viewModel, { route = Route.ERROR_CODES }, { route = Route.GUIDE }, { route = Route.A2L })
                    MainTab.SETTINGS -> LaunchSettingsScreen(viewModel) { showPlans = true }
                }
            }
        }
    }
    if (showPlans) SubscriptionBottomSheet(tier, viewModel::setTier) { showPlans = false }
}

@Composable
private fun FieldToolsScreen(viewModel: HvacViewModel) {
    var tool by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(tool) {
            Tab(tool == 0, { tool = 0 }, text = { Text("Calculators") })
            Tab(tool == 1, { tool = 1 }, text = { Text("PT reference") })
        }
        if (tool == 0) CalculatorsScreen(viewModel) else PTChartScreen(viewModel)
    }
}
