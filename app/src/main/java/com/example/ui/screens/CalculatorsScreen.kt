package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.ui.SubcoolingVerdict
import com.example.ui.SuperheatVerdict
import com.example.ui.components.FieldMetricCard
import com.example.ui.theme.AlertRed
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FrostCyanLight
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.HvacBlue
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlin.math.roundToInt

@Composable
fun CalculatorsScreen(
    viewModel: HvacViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Superheat", "Subcooling", "CFM Airflow", "Duct Sizing")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text("Reference estimates. Verify PT data and targets with the manufacturer. CFM assumes standard-density air; electric heat assumes single-phase resistive load.", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate900,
            contentColor = FrostCyanLight,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = FrostCyanLight,
                    height = 3.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("calc_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.5.sp
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> SuperheatCalculatorTab(viewModel)
                1 -> SubcoolingCalculatorTab(viewModel)
                2 -> CfmCalculatorTab(viewModel)
                3 -> DuctSizingTab(viewModel)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 1. SUPERHEAT TAB
// -----------------------------------------------------------------------------
@Composable
private fun SuperheatCalculatorTab(viewModel: HvacViewModel) {
    val selectedRef by viewModel.selectedShRefrigerant.collectAsState()
    val suctionPsig by viewModel.shSuctionPressure.collectAsState()
    val suctionTemp by viewModel.shSuctionTemp.collectAsState()
    val outdoorDb by viewModel.shOutdoorDryBulb.collectAsState()
    val indoorWb by viewModel.shIndoorWetBulb.collectAsState()
    val useFixedTarget by viewModel.shUseFixedTarget.collectAsState()
    val fixedTarget by viewModel.shFixedTarget.collectAsState()

    val calculation = runCatching { viewModel.calculateSuperheat() }
    val result = calculation.getOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Refrigerant selector row
        Text(
            text = "Select Refrigerant",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(RefrigerantData.allRefrigerants) { ref ->
                FilterChip(
                    selected = selectedRef.id == ref.id,
                    onClick = { viewModel.selectedShRefrigerant.value = ref },
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

        // Live Diagnostic Verdict Card
        if (result == null) Text(calculation.exceptionOrNull()?.message ?: "Check inputs", color = MaterialTheme.colorScheme.error)
        else {
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUPERHEAT DIAGNOSTIC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    val verdictColor = when (result.verdict) {
                        SuperheatVerdict.IN_TARGET -> GaugeGreen
                        SuperheatVerdict.TOO_HIGH -> AlertRed
                        SuperheatVerdict.TOO_LOW -> FlameOrange
                    }
                    val verdictText = when (result.verdict) {
                        SuperheatVerdict.IN_TARGET -> "WITHIN TARGET BAND"
                        SuperheatVerdict.TOO_HIGH -> "HIGH SUPERHEAT"
                        SuperheatVerdict.TOO_LOW -> "LOW SUPERHEAT"
                    }
                    Surface(
                        color = verdictColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = verdictText,
                            color = verdictColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FieldMetricCard(
                        label = "Actual Superheat",
                        value = "${result.actualSuperheatF}°",
                        unit = "F",
                        statusColor = FrostCyanLight,
                        subtext = "Pipe Temp - Sat Temp",
                        modifier = Modifier.weight(1f)
                    )
                    FieldMetricCard(
                        label = "Target Superheat",
                        value = "${result.targetSuperheatF}°",
                        unit = "F",
                        statusColor = MaterialTheme.colorScheme.secondary,
                        subtext = if (useFixedTarget) "Fixed TXV" else "Piston Formula",
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Evaporator Saturation Temp: ${result.evapSatTempF}°F @ ${suctionPsig} psig",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }

        // Field Inputs
        Text(
            text = "Field Pressure & Temperature",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = suctionPsig,
                onValueChange = { viewModel.shSuctionPressure.value = it },
                label = { Text("Suction Pressure (psig)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_sh_pressure"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FrostCyanLight)
            )
            OutlinedTextField(
                value = suctionTemp,
                onValueChange = { viewModel.shSuctionTemp.value = it },
                label = { Text("Suction Line Temp (°F)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_sh_temp"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FrostCyanLight)
            )
        }

        // Target Superheat Mode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Target Mode: ${if (useFixedTarget) "Fixed TXV" else "Piston (Non-TXV)"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            FilterChip(
                selected = useFixedTarget,
                onClick = { viewModel.shUseFixedTarget.value = !useFixedTarget },
                label = { Text(if (useFixedTarget) "Switch to Piston" else "Switch to TXV") }
            )
        }

        if (!useFixedTarget) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = indoorWb,
                    onValueChange = { viewModel.shIndoorWetBulb.value = it },
                    label = { Text("Indoor Wet Bulb (°F)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = outdoorDb,
                    onValueChange = { viewModel.shOutdoorDryBulb.value = it },
                    label = { Text("Outdoor Dry Bulb (°F)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Piston formula: Target SH = ((3 × Indoor WB) - 80 - Outdoor DB) ÷ 2",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            OutlinedTextField(
                value = fixedTarget,
                onValueChange = { viewModel.shFixedTarget.value = it },
                label = { Text("Manufacturer Specified Superheat (°F)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 2. SUBCOOLING TAB
// -----------------------------------------------------------------------------
@Composable
private fun SubcoolingCalculatorTab(viewModel: HvacViewModel) {
    val selectedRef by viewModel.selectedScRefrigerant.collectAsState()
    val liquidPsig by viewModel.scLiquidPressure.collectAsState()
    val liquidTemp by viewModel.scLiquidTemp.collectAsState()
    val targetSc by viewModel.scTarget.collectAsState()

    val calculation = runCatching { viewModel.calculateSubcooling() }
    val result = calculation.getOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Select Refrigerant",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(RefrigerantData.allRefrigerants) { ref ->
                FilterChip(
                    selected = selectedRef.id == ref.id,
                    onClick = { viewModel.selectedScRefrigerant.value = ref },
                    label = { Text(ref.name, fontSize = 12.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(ref.displayColor, CircleShape)
                        )
                    }
                )
            }
        }

        if (result == null) Text(calculation.exceptionOrNull()?.message ?: "Check inputs", color = MaterialTheme.colorScheme.error)
        else {
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUBCOOLING STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    val verdictColor = when (result.verdict) {
                        SubcoolingVerdict.IN_TARGET -> GaugeGreen
                        SubcoolingVerdict.TOO_HIGH -> AlertRed
                        SubcoolingVerdict.TOO_LOW -> FlameOrange
                    }
                    val verdictText = when (result.verdict) {
                        SubcoolingVerdict.IN_TARGET -> "WITHIN TARGET BAND"
                        SubcoolingVerdict.TOO_HIGH -> "HIGH SUBCOOLING"
                        SubcoolingVerdict.TOO_LOW -> "LOW SUBCOOLING"
                    }
                    Surface(
                        color = verdictColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = verdictText,
                            color = verdictColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FieldMetricCard(
                        label = "Actual Subcooling",
                        value = "${result.actualSubcoolingF}°",
                        unit = "F",
                        statusColor = FrostCyanLight,
                        subtext = "Sat Temp - Liquid Temp",
                        modifier = Modifier.weight(1f)
                    )
                    FieldMetricCard(
                        label = "Target Subcooling",
                        value = "${result.targetSubcoolingF}°",
                        unit = "F",
                        statusColor = MaterialTheme.colorScheme.secondary,
                        subtext = "Entered target",
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Condenser Saturation Temp: ${result.condSatTempF}°F @ ${liquidPsig} psig",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }

        Text(
            text = "Liquid Line Gauge & Thermometer",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = liquidPsig,
                onValueChange = { viewModel.scLiquidPressure.value = it },
                label = { Text("Liquid Pressure (psig)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_sc_pressure")
            )
            OutlinedTextField(
                value = liquidTemp,
                onValueChange = { viewModel.scLiquidTemp.value = it },
                label = { Text("Liquid Line Temp (°F)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_sc_temp")
            )
        }

        OutlinedTextField(
            value = targetSc,
            onValueChange = { viewModel.scTarget.value = it },
            label = { Text("Target Subcooling from Unit Data Plate (°F)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// -----------------------------------------------------------------------------
// 3. CFM TAB
// -----------------------------------------------------------------------------
@Composable
private fun CfmCalculatorTab(viewModel: HvacViewModel) {
    val mode by viewModel.cfmMode.collectAsState()
    val btu by viewModel.cfmBtu.collectAsState()
    val deltaT by viewModel.cfmDeltaT.collectAsState()
    val tons by viewModel.cfmTons.collectAsState()
    val perTon by viewModel.cfmCfmPerTon.collectAsState()
    val volts by viewModel.cfmVolts.collectAsState()
    val amps by viewModel.cfmAmps.collectAsState()

    val calculation = runCatching { viewModel.calculateCfm() }
    val result = calculation.getOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Airflow Calculation Formula",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Sensible Heat", "Cooling Tonnage", "Electric Strip").forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { viewModel.cfmMode.value = m },
                    label = { Text(m, fontSize = 11.5.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (result == null) Text(calculation.exceptionOrNull()?.message ?: "Check inputs", color = MaterialTheme.colorScheme.error)
        else {
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ESTIMATED AIRFLOW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${result.cfm} CFM",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FrostCyanLight
                )
                Text(
                    text = result.formula,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        }

        when (mode) {
            "Sensible Heat" -> {
                OutlinedTextField(
                    value = btu,
                    onValueChange = { viewModel.cfmBtu.value = it },
                    label = { Text("Sensible Heat Capacity (BTU/h)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deltaT,
                    onValueChange = { viewModel.cfmDeltaT.value = it },
                    label = { Text("Absolute temperature difference ΔT (°F)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Cooling Tonnage" -> {
                OutlinedTextField(
                    value = tons,
                    onValueChange = { viewModel.cfmTons.value = it },
                    label = { Text("Nominal Cooling Tons (e.g. 3.0)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = perTon,
                    onValueChange = { viewModel.cfmCfmPerTon.value = it },
                    label = { Text("CFM per Ton (Typically 350-400 CFM)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Electric Strip" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = volts,
                        onValueChange = { viewModel.cfmVolts.value = it },
                        label = { Text("Line Voltage (V)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amps,
                        onValueChange = { viewModel.cfmAmps.value = it },
                        label = { Text("Heater Current (A)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = deltaT,
                    onValueChange = { viewModel.cfmDeltaT.value = it },
                    label = { Text("Heater Temp Rise ΔT (°F)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. DUCT SIZING TAB
// -----------------------------------------------------------------------------
@Composable
private fun DuctSizingTab(viewModel: HvacViewModel) {
    val cfm by viewModel.ductCfm.collectAsState()
    val friction by viewModel.ductFriction.collectAsState()
    val rectWidth by viewModel.ductRectWidth.collectAsState()

    val calculation = runCatching { viewModel.calculateDuct() }
    val result = calculation.getOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (result == null) Text(calculation.exceptionOrNull()?.message ?: "Check inputs", color = MaterialTheme.colorScheme.error)
        else {
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DUCT ESTIMATES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FieldMetricCard(
                        label = "Round Duct",
                        value = "${result.roundDiameterInches}\"",
                        unit = "dia",
                        statusColor = FrostCyanLight,
                        subtext = "Circular Sheetmetal",
                        modifier = Modifier.weight(1f)
                    )
                    FieldMetricCard(
                        label = "Rectangular",
                        value = "${result.rectWidthInches}×${result.rectHeightInches.roundToInt()}\"",
                        unit = "W×H",
                        statusColor = MaterialTheme.colorScheme.secondary,
                        subtext = "Area only; not equal friction",
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = if (result.velocityFpm > 900) AlertRed.copy(alpha = 0.2f) else Slate800,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Duct Velocity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${result.velocityFpm} FPM",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (result.velocityFpm > 900) AlertRed else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.velocityAssessment,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
        }

        Text(
            text = "Approximate duct inputs — validate sizing with an approved design method",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = cfm,
            onValueChange = { viewModel.ductCfm.value = it },
            label = { Text("Required Airflow (CFM)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = friction,
                onValueChange = { viewModel.ductFriction.value = it },
                label = { Text("Friction Rate (in. w.c./100ft)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = rectWidth,
                onValueChange = { viewModel.ductRectWidth.value = it },
                label = { Text("Desired Width (in)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Standard friction rates: Supply Trunk: 0.08\" to 0.10\" w.c. / Return Trunk: 0.05\" to 0.08\" w.c.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
