package com.example.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.HvacViewModel
@Composable
fun HomeScreen(viewModel: HvacViewModel, onNavigateToTab: (Int) -> Unit, onOpenSubscriptionModal: () -> Unit, modifier: Modifier = Modifier) {
    val tier by viewModel.currentTier.collectAsState()
    val onboarded by viewModel.onboardingCompleted.collectAsState()
    LazyColumn(modifier.fillMaxSize().testTag("home_tools"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Your field toolkit", style = MaterialTheme.typography.headlineMedium)
            Text("Calculate a measurement or save a service note.")
            if (BuildConfig.DEBUG) Text("TEST BUILD · Preview paid tiers from Your plan.", color = MaterialTheme.colorScheme.secondary)
        }
        if (!onboarded) item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("Start with one job", style = MaterialTheme.typography.titleMedium)
                Text("Use Calculators for measurements and Logbook to keep a note. Reference tools do not diagnose or certify equipment.")
                Button(onClick = { viewModel.completeOnboarding(); onNavigateToTab(1) }) { Text("Open calculators") }
                TextButton(onClick = viewModel::completeOnboarding) { Text("Got it") }
            } }
        }
        item { Text("Daily tools", style = MaterialTheme.typography.titleMedium) }
        item { HomeTool("Calculators", "Superheat, subcooling, airflow and duct estimates", "FREE", "home_tool_calculators") { onNavigateToTab(1) } }
        item { HomeTool("PT reference", "Approximate pressure-temperature tables", "FREE", "home_tool_pt") { onNavigateToTab(2) } }
        item { HomeTool("Service logbook", "Save, search and favorite service notes", "FREE", "home_tool_logbook") { onNavigateToTab(7) } }
        item { Text("Reference & specialist tools", style = MaterialTheme.typography.titleMedium) }
        item { HomeTool("Error-code reference", "Model-dependent fault examples", if (tier.canAccessErrorCodes) "OPEN" else "PLUS", "home_tool_errors") { onNavigateToTab(4) } }
        item { HomeTool("A2L charge worksheet", "Charge arithmetic; no compliance certification", if (tier.canAccessA2LCompliance) "OPEN" else "PLUS", "home_tool_a2l") { onNavigateToTab(5) } }
        item { HomeTool("Offline field guide", "Keyword-selected troubleshooting checklists", if (tier.canAccessSeniorTechAI) "OPEN" else "PLUS", "home_tool_guide") { onNavigateToTab(3) } }
        item { HomeTool("Hardware tools", "Read labels; browse parts and circuit examples", if (tier.canAccessDataPlateScanner) "OPEN" else "PRO", "home_tool_hardware") { onNavigateToTab(6) } }
        item { HomeTool("Blink-code examples", "Stored examples; no video analysis", "FREE", "home_tool_video") { onNavigateToTab(8) } }
        item { OutlinedButton(onClick = onOpenSubscriptionModal, modifier = Modifier.fillMaxWidth().testTag("tier_switcher_chip")) { Text("Your plan · ${tier.title}") } }
    }
}
@Composable
private fun HomeTool(title: String, detail: String, badge: String, tag: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } }
}
