package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ErrorCode
import com.example.data.model.ErrorSeverity
import com.example.data.model.UserTier
import com.example.ui.HvacViewModel
import com.example.ui.components.LockedFeatureGate
import com.example.ui.theme.AlertRed
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FrostCyanLight
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun ErrorCodesScreen(
    viewModel: HvacViewModel,
    onOpenSubscriptionModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTier by viewModel.currentTier.collectAsState()
    val isUnlocked = currentTier.canAccessErrorCodes

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isUnlocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                LockedFeatureGate(
                    requiredTier = UserTier.PLUS_USER,
                    featureName = "Error-code reference examples",
                    valueProp = "Browse model-dependent fault examples. Manufacturer verification is required.",
                    onUpgradeClick = onOpenSubscriptionModal
                )
            }
        } else {
            ErrorCodesContent(viewModel = viewModel)
        }
    }
}

@Composable
private fun ErrorCodesContent(viewModel: HvacViewModel) {
    val errorCodes by viewModel.filteredErrorCodes.collectAsState()
    val searchQuery by viewModel.errorSearchQuery.collectAsState()
    val selectedBrand by viewModel.errorSelectedBrand.collectAsState()

    var selectedEquipment by rememberSaveable { mutableStateOf("All") }
    var selectedBlinkPattern by rememberSaveable { mutableStateOf("All") }

    val brands = listOf("All", "Carrier", "Goodman", "Trane", "Lennox", "Daikin", "Mitsubishi", "Rheem")
    val equipmentList = listOf("All") + com.example.data.model.ErrorCodeDatabase.entries.map { it.equipmentCategory }.distinct()
    val blinkPatterns = listOf("All") + com.example.data.model.ErrorCodeDatabase.entries.map { it.codeDisplay }.distinct()

    val visibleCodes = errorCodes.filter { code ->
        val matchesEquip = selectedEquipment == "All" || code.equipmentCategory.contains(selectedEquipment, ignoreCase = true)
        val matchesBlink = selectedBlinkPattern == "All" ||
            code.codeDisplay.contains(selectedBlinkPattern, ignoreCase = true) ||
            code.flashPattern.contains(selectedBlinkPattern, ignoreCase = true)
        matchesEquip && matchesBlink
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Error-code reference examples",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Unverified examples. Check the exact model and board revision.",
            style = MaterialTheme.typography.bodySmall,
            color = FlameOrange,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setErrorSearchQuery(it) },
            placeholder = { Text("Search code (e.g. '33', '3 Flashes', 'E6', 'Lockout')...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = FrostCyanLight)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setErrorSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("error_code_search_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Step 1: Brand Selection
        Text(
            text = "1. Select Brand:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(brands) { brand ->
                FilterChip(
                    selected = selectedBrand == brand,
                    onClick = { viewModel.setErrorSelectedBrand(brand) },
                    label = { Text(brand, fontSize = 11.5.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Step 2: Equipment Selection
        Text(
            text = "2. Select Equipment Type:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(equipmentList) { equip ->
                FilterChip(
                    selected = selectedEquipment == equip,
                    onClick = { selectedEquipment = equip },
                    label = { Text(equip, fontSize = 11.5.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Step 3: Blink Pattern Selection
        Text(
            text = "3. Select Blink Pattern / Code:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(blinkPatterns) { blink ->
                FilterChip(
                    selected = selectedBlinkPattern == blink,
                    onClick = { selectedBlinkPattern = blink },
                    label = { Text(blink, fontSize = 11.5.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (visibleCodes.isEmpty()) item {
                Text("No examples match these filters.")
                androidx.compose.material3.TextButton(onClick = { selectedEquipment = "All"; selectedBlinkPattern = "All"; viewModel.setErrorSelectedBrand("All"); viewModel.setErrorSearchQuery("") }) { Text("Clear filters") }
            }
            items(visibleCodes, key = { it.id }) { code ->
                ErrorCodeCard(code = code)
            }
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun ErrorCodeCard(code: ErrorCode) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_card_${code.id}")
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = FrostCyanLight.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = code.brand,
                            color = FrostCyanLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    val severityColor = when (code.severity) {
                        ErrorSeverity.HARD_LOCKOUT -> AlertRed
                        ErrorSeverity.SOFT_LOCKOUT -> FlameOrange
                        ErrorSeverity.WARNING -> Color(0xFFFBBF24)
                        ErrorSeverity.DIAGNOSTIC_INFO -> GaugeGreen
                    }

                    Surface(
                        color = severityColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = code.severity.name.replace("_", " "),
                            color = severityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = code.codeDisplay,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlameOrange
                )
                Text(
                    text = code.equipmentCategory,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = code.faultTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Flash Signal: ${code.flashPattern}",
                style = MaterialTheme.typography.bodySmall,
                color = FrostCyanLight,
                fontSize = 12.sp
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "PROBABLE CAUSES:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            code.probableCauses.forEach { cause ->
                                Text(
                                    text = "• $cause",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "FIELD DIAGNOSTIC CHECKS:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = FrostCyanLight
                            )
                            code.fieldDiagnosticChecks.forEach { check ->
                                Text(
                                    text = "▶ $check",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Surface(
                        color = GaugeGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "REFERENCE NOTE:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = GaugeGreen
                            )
                            Text(
                                text = code.verifiedFix,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.5.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (code.boardTerminalNote.isNotBlank()) {
                        Text(
                            text = "Terminal Note: ${code.boardTerminalNote}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
