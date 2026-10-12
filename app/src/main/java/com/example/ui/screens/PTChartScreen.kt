package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RefrigerantData
import com.example.ui.HvacViewModel
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FrostCyanLight
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlin.math.roundToInt

@Composable
fun PTChartScreen(
    viewModel: HvacViewModel,
    modifier: Modifier = Modifier
) {
    val selectedRef by viewModel.ptSelectedRefrigerant.collectAsState()
    val pressureStr by viewModel.ptGaugePressure.collectAsState()
    val pressureVal = pressureStr.trim().toDoubleOrNull()
    val conversion = runCatching { require(pressureVal != null) { "Enter pressure in psig" }; selectedRef.satTempFromPsig(pressureVal) }
    val satTemp = conversion.getOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Refrigerant Pressure-Temperature (PT)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Approximate reference tables, not verified for charging. Blends require separate dew/bubble data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Refrigerant selector
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RefrigerantData.allRefrigerants) { ref ->
                    FilterChip(
                        selected = selectedRef.id == ref.id,
                        onClick = {
                            viewModel.ptSelectedRefrigerant.value = ref
                            viewModel.ptGaugePressure.value = ref.typicalEvapPressurePsig.toString()
                        },
                        label = { Text(ref.name, fontSize = 12.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(ref.displayColor, CircleShape)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FrostCyanLight.copy(alpha = 0.2f),
                            selectedLabelColor = FrostCyanLight
                        )
                    )
                }
            }
        }

        // Live Gauge Readout Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, selectedRef.displayColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = selectedRef.displayColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = selectedRef.safetyClass,
                                color = selectedRef.displayColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "GWP: ${selectedRef.gwp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "SATURATION TEMPERATURE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (satTemp == null) "—" else "${satTemp}°F",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FrostCyanLight
                    )

                    Text(
                        text = if (satTemp == null) (conversion.exceptionOrNull()?.message ?: "Check input") else "@ $pressureStr psig",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedRef.glideF > 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = FlameOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Zeotropic Blend Glide: ~${selectedRef.glideF}°F (Charge in Liquid state only)",
                                style = MaterialTheme.typography.bodySmall,
                                color = FlameOrange,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Pressure Slider & Input
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dial Gauge Pressure (psig)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Slider(
                        value = (pressureVal?.takeIf { it.isFinite() }?.toFloat() ?: selectedRef.samplePoints.first().first.toFloat()).coerceIn(selectedRef.samplePoints.first().first.toFloat(), selectedRef.samplePoints.last().first.toFloat()),
                        onValueChange = { viewModel.ptGaugePressure.value = it.roundToInt().toString() },
                        valueRange = selectedRef.samplePoints.first().first.toFloat()..selectedRef.samplePoints.last().first.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = FrostCyanLight,
                            activeTrackColor = FrostCyanLight,
                            inactiveTrackColor = Slate700
                        ),
                        modifier = Modifier.testTag("pt_pressure_slider")
                    )

                    OutlinedTextField(
                        value = pressureStr,
                        onValueChange = { viewModel.ptGaugePressure.value = it },
                        label = { Text("Exact Gauge Reading (psig)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pt_pressure_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Quick Field Benchmarks:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickPressureChip(
                            label = "40°F Evap",
                            targetTempF = 40.0,
                            refrigerant = selectedRef,
                            onSelect = { viewModel.ptGaugePressure.value = it.roundToInt().toString() },
                            modifier = Modifier.weight(1f)
                        )
                        QuickPressureChip(
                            label = "32°F Freezing",
                            targetTempF = 32.0,
                            refrigerant = selectedRef,
                            onSelect = { viewModel.ptGaugePressure.value = it.roundToInt().toString() },
                            modifier = Modifier.weight(1f)
                        )
                        QuickPressureChip(
                            label = "110°F Cond",
                            targetTempF = 110.0,
                            refrigerant = selectedRef,
                            onSelect = { viewModel.ptGaugePressure.value = it.roundToInt().toString() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Static PT Reference Table Section
        item {
            Text(
                text = "${selectedRef.id} Calibration Table",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pressure (psig)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Sat Vapor Temp (°F)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Phase State",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    selectedRef.samplePoints.forEach { (psig, tempF) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$psig psig",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "$tempF °F",
                                fontWeight = FontWeight.SemiBold,
                                color = FrostCyanLight,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (tempF < 50.0) "Evaporator" else "Condenser",
                                color = if (tempF < 50.0) FrostCyanLight.copy(alpha = 0.7f) else FlameOrange.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPressureChip(
    label: String,
    targetTempF: Double,
    refrigerant: com.example.data.model.Refrigerant,
    onSelect: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val psig = refrigerant.psigFromTempF(targetTempF)
    Surface(
        color = Slate700,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.clickable { onSelect(psig) }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${psig.roundToInt()} psig",
                fontSize = 10.sp,
                color = FrostCyanLight
            )
        }
    }
}
