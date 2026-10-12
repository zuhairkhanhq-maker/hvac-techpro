package com.example.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.VideoDiagnosticDatabase
import com.example.ui.HvacViewModel
@Composable
fun VideoDiagnosticScreen(viewModel: HvacViewModel, onOpenSubscriptionModal: () -> Unit, modifier: Modifier = Modifier) {
    val selected by viewModel.videoScanResult.collectAsState()
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Blink-code examples", style = MaterialTheme.typography.titleLarge); Text("Stored educational examples, free on every plan. No video is recorded or analyzed and no credits are used. Verify codes in the exact model service manual.") }
        selected?.let { sample -> item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("Example: ${sample.brand}", style = MaterialTheme.typography.titleMedium)
            Text(sample.matchedCode); Text(sample.faultSummary)
            Text("Sample: ${sample.simulatedPulses} pulses · ${sample.lightColor}")
            Text("No fault has been detected on your equipment.")
            TextButton(onClick = viewModel::clearVideoScanResult) { Text("Close example") }
        } } } }
        items(VideoDiagnosticDatabase.samples, key = { it.id }) { sample -> Card(Modifier.fillMaxWidth().testTag("video_sample_${sample.id}").clickable { viewModel.runVideoDiagnostic(sample) }) { Column(Modifier.padding(16.dp)) {
            Text(sample.brand, style = MaterialTheme.typography.titleMedium); Text(sample.title); Text("View example", color = MaterialTheme.colorScheme.primary)
        } } }
    }
}
