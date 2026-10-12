package com.example.ui.screens
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.UserTier
import com.example.ui.HvacViewModel
import com.example.ui.components.LockedFeatureGate
import java.io.File
@Composable
fun SchematicScannerScreen(viewModel: HvacViewModel, onOpenSubscriptionModal: () -> Unit, modifier: Modifier = Modifier) {
    val tier by viewModel.currentTier.collectAsState()
    if (tier.canAccessSchematicScanner) LabelScanner(viewModel, modifier)
    else LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) { item { LockedFeatureGate(UserTier.PRO_USER, "Label text recognition", "Read a clear label photo on your device. Verify every digit and unit.", onOpenSubscriptionModal) } }
}
@Composable
fun LabelScanner(viewModel: HvacViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val result by viewModel.scannerResult.collectAsState(); val scanning by viewModel.isScanning.collectAsState()
    var captureUri by rememberSaveable { mutableStateOf<String?>(null) }; var error by rememberSaveable { mutableStateOf<String?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) { error = null; viewModel.scanLabel(uri) } }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = captureUri?.let(Uri::parse)
        if (success && uri != null) { error = null; viewModel.scanLabel(uri) } else error = "Photo cancelled. Take or choose a photo to continue."
        captureUri = null
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Read a label", style = MaterialTheme.typography.titleLarge); Text("Capture the whole label in focus, without glare. On-device OCR reads text; it does not identify replacement parts or trace wiring.") }
        item {
            Button(enabled = !scanning, modifier = Modifier.fillMaxWidth().testTag("camera_ocr_btn"), onClick = {
                try {
                    error = null; viewModel.clearScanResult()
                    val dir = File(context.cacheDir, "label_photos").apply { mkdirs() }
                    dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000L }?.forEach { it.delete() }
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File.createTempFile("label_", ".jpg", dir))
                    captureUri = uri.toString(); camera.launch(uri)
                } catch (_: Exception) { error = "Camera unavailable. Choose a saved photo instead."; captureUri = null }
            }) { Text("Take label photo") }
            OutlinedButton(enabled = !scanning, modifier = Modifier.fillMaxWidth(), onClick = {
                try { error = null; viewModel.clearScanResult(); gallery.launch("image/*") } catch (_: Exception) { error = "Image picker unavailable." }
            }) { Text("Choose photo") }
        }
        if (scanning) item { CircularProgressIndicator(); Text("Reading label…") }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        result?.let { text -> item { Card(Modifier.fillMaxWidth()) { SelectionContainer { Text(text, Modifier.padding(16.dp)) } }; TextButton(onClick = viewModel::clearScanResult) { Text("Clear result") } } }
    }
}
