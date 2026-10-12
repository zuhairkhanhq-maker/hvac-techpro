package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DataPlateSpec
import com.example.data.model.HardwareDatabase
import com.example.data.model.SchematicCircuitPath
import com.example.data.model.UserTier
import com.example.ui.HvacViewModel
import com.example.ui.components.FieldMetricCard
import com.example.ui.components.LockedFeatureGate
import com.example.ui.theme.AlertRed
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FrostCyanLight
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun HardwareCommandScreen(
    viewModel: HvacViewModel,
    onOpenSubscriptionModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTier by viewModel.currentTier.collectAsState()
    val isUnlocked = currentTier.canAccessDataPlateScanner

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isUnlocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                LockedFeatureGate(
                    requiredTier = UserTier.PRO_USER,
                    featureName = "Hardware tools",
                    valueProp = "Read label text from a clear photo. Parts and circuit catalogs are examples; automatic matching and tracing are unavailable.",
                    onUpgradeClick = onOpenSubscriptionModal
                )
            }
        } else {
            HardwareCommandContent(viewModel)
        }
    }
}

@Composable
private fun HardwareCommandContent(viewModel: HvacViewModel) {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    val sections = listOf("Read label", "Parts examples", "Circuit examples")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedSection,
            containerColor = Slate900,
            contentColor = GaugeGreen,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSection]),
                    color = GaugeGreen,
                    height = 3.dp
                )
            }
        ) {
            sections.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSection == index,
                    onClick = { selectedSection = index },
                    modifier = Modifier.testTag("hardware_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.5.sp
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSection) {
                0 -> LabelScanner(viewModel)
                1 -> AftermarketPartsSection()
                2 -> SchematicHighlighterSection()
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 1. DATA PLATE OCR SECTION
// -----------------------------------------------------------------------------
// -----------------------------------------------------------------------------
// 2. AFTERMARKET PART MATCHER
// -----------------------------------------------------------------------------
@Composable
private fun AftermarketPartsSection() {
    val sampleList = HardwareDatabase.sampleDataPlates
    var selectedPlate by remember { mutableStateOf(sampleList.first()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Parts reference examples",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Unverified sample catalog. Confirm compatibility with the manufacturer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Text(
                text = "Example equipment: ${selectedPlate.unitTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = FrostCyanLight,
                fontWeight = FontWeight.SemiBold
            )
        }

        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(sampleList) { plate ->
            FilterChip(selectedPlate.id == plate.id, { selectedPlate = plate }, label = { Text(plate.brand) })
        } } }
        items(selectedPlate.universalParts) { part ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = FlameOrange.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = part.category,
                                color = FlameOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Unverified example",
                            color = GaugeGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = part.universalReplacement,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = "OEM Part #: ${part.oemNumber} • Supplier: ${part.manufacturer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = FrostCyanLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Field Wiring: ${part.wiringNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// 3. SCHEMATIC PATH HIGHLIGHTER
// -----------------------------------------------------------------------------
@Composable
private fun SchematicHighlighterSection() {
    val circuits = HardwareDatabase.schematicCircuits
    var selectedCircuit by remember { mutableStateOf(circuits.first()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Schematic Circuit Path Highlighter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Generic examples, not traced from your photo. Use the exact equipment diagram.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Circuit Selector Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(circuits) { circuit ->
                    FilterChip(
                        selected = selectedCircuit.id == circuit.id,
                        onClick = { selectedCircuit = circuit },
                        label = { Text(circuit.voltageClass, fontSize = 11.5.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(circuit.traceColor, CircleShape)
                            )
                        }
                    )
                }
            }
        }

        // Active Circuit Trace HUD Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, selectedCircuit.traceColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GENERIC CIRCUIT EXAMPLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = selectedCircuit.traceColor,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            color = selectedCircuit.traceColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = selectedCircuit.voltageClass,
                                color = selectedCircuit.traceColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = selectedCircuit.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    // Visual Node Flow
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate800, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ACTIVE PATH TRACE NODES:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        selectedCircuit.wireNodes.forEachIndexed { idx, node ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(selectedCircuit.traceColor, CircleShape)
                                )
                                Text(
                                    text = "${idx + 1}. $node",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Multimeter Probe Instructions
                    Surface(
                        color = FrostCyanLight.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "MULTIMETER PROBE CHECKPOINT:",
                                style = MaterialTheme.typography.labelSmall,
                                color = FrostCyanLight,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedCircuit.meterTestPoint,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Expected Normal: ${selectedCircuit.expectedReading}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GaugeGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Failure Diagnosis
                    Surface(
                        color = AlertRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "COMMON FAULT SIGN:",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlertRed,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedCircuit.commonFailure,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
