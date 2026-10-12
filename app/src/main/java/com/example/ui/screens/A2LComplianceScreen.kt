package com.example.ui.screens
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.HvacViewModel
import com.example.ui.components.LockedFeatureGate
@Composable
fun A2LComplianceScreen(viewModel: HvacViewModel, onOpenSubscriptionModal: () -> Unit, modifier: Modifier = Modifier) {
    val tier by viewModel.currentTier.collectAsState()
    val ref by viewModel.a2lSelectedRefrigerant.collectAsState()
    val area by viewModel.a2lRoomArea.collectAsState(); val height by viewModel.a2lCeilingHeight.collectAsState()
    val charge by viewModel.a2lFactoryChargeLbs.collectAsState(); val length by viewModel.a2lLineSetLengthFt.collectAsState()
    val rate by viewModel.a2lLineOzPerFt.collectAsState(); val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!tier.canAccessA2LCompliance) item { LockedFeatureGate(UserTier.PLUS_USER, "A2L charge worksheet", "Calculate charge arithmetic and copy a note. This tool does not certify compliance.", onOpenSubscriptionModal) }
        else {
            item { Text("A2L charge worksheet", style = MaterialTheme.typography.titleLarge); Text("Room volume alone cannot establish code compliance. Use manufacturer charge inputs.") }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { A2LRefrigerants.list.forEach { option -> FilterChip(ref == option, { viewModel.a2lSelectedRefrigerant.value = option }, label = { Text(option.name) }) } } }
            item { ChargeInput("Room floor area (ft²)", area) { viewModel.a2lRoomArea.value = it } }
            item { ChargeInput("Ceiling height (ft)", height) { viewModel.a2lCeilingHeight.value = it } }
            item { ChargeInput("Factory charge (lb)", charge) { viewModel.a2lFactoryChargeLbs.value = it } }
            item { ChargeInput("Additional chargeable line length (ft)", length) { viewModel.a2lLineSetLengthFt.value = it }; Text("Exclude line length already included in the factory charge.", style = MaterialTheme.typography.bodySmall) }
            item { ChargeInput("Manufacturer charge rate (oz/ft)", rate) { viewModel.a2lLineOzPerFt.value = it } }
            item {
                val calculation = runCatching { viewModel.getA2LComplianceResult() }; val result = calculation.getOrNull()
                if (result == null) Text(calculation.exceptionOrNull()?.message ?: "Check measurements", color = MaterialTheme.colorScheme.error)
                else Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text("Estimated total: %.3f lb".format(java.util.Locale.US, result.totalSystemChargeLbs), style = MaterialTheme.typography.titleLarge)
                    Text("Room volume: ${result.roomVolumeCuFt} ft³")
                    Text("Compliance: not evaluated. Confirm installation limits and mitigation with the manufacturer and local inspector.")
                    Button(onClick = {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Charge worksheet", result.generateCertificate("", "", "", "")))
                        Toast.makeText(context, "Worksheet copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy worksheet") }
                } }
            }
        }
    }
}
@Composable
private fun ChargeInput(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}
