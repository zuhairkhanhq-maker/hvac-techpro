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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TroubleshootingLog
import com.example.ui.HvacViewModel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FrostCyanLight
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun BrainDumpScreen(
    viewModel: HvacViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.troubleshootingLogs.collectAsState()
    val searchQuery by viewModel.logSearchQuery.collectAsState()
    val filterBrand by viewModel.logFilterBrand.collectAsState()

    val saving by viewModel.savingLog.collectAsState()
    var pendingDelete by remember { mutableStateOf<TroubleshootingLog?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedLogDetail by remember { mutableStateOf<TroubleshootingLog?>(null) }

    val brands = listOf("All", "Carrier", "Goodman", "Trane", "Lennox", "Universal")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = FrostCyanLight,
                contentColor = Slate900,
                modifier = Modifier.testTag("add_log_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Field Log")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Service logbook",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "100% Offline Device Storage • Your Personal Field Fixes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setLogSearchQuery(it) },
                placeholder = { Text("Search symptom, brand, code, or fix...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = FrostCyanLight)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setLogSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_logs_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Brand Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(brands) { brand ->
                    FilterChip(
                        selected = filterBrand == brand,
                        onClick = { viewModel.setLogFilterBrand(brand) },
                        label = { Text(brand, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = null,
                            tint = Slate700,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No troubleshooting logs found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap + to write down your unique field fix!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        TroubleshootingLogCard(
                            log = log,
                            onCardClick = { selectedLogDetail = log },
                            onToggleFavorite = { viewModel.toggleFavorite(log) },
                            onDelete = { pendingDelete = log }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    pendingDelete?.let { log -> AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("Delete this log?") }, text = { Text(log.title) },
        confirmButton = { TextButton(onClick = { viewModel.deleteLog(log.id); pendingDelete = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep log") } }) }
    // Detail Dialog
    if (selectedLogDetail != null) {
        val log = selectedLogDetail!!
        AlertDialog(
            onDismissRequest = { selectedLogDetail = null },
            title = {
                Text(
                    text = log.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = FrostCyanLight.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = log.brand,
                                color = FrostCyanLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (log.errorCode.isNotBlank()) {
                            Surface(
                                color = FlameOrange.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = log.errorCode,
                                    color = FlameOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Symptom *:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = log.symptom, fontSize = 13.5.sp)

                    Text(
                        text = "Root Cause:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = log.rootCause, fontSize = 13.5.sp)

                    Text(
                        text = "Verified Field Fix:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = GaugeGreen
                    )
                    Text(text = log.resolution, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                    if (log.customerTag.isNotBlank()) {
                        Text(
                            text = "Site Tag: ${log.customerTag}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLogDetail = null }) {
                    Text("Close", color = FrostCyanLight)
                }
            },
            containerColor = Slate900
        )
    }

    // Add Log Dialog
    if (showAddDialog) {
        AddLogDialog(
            saving = saving,
            onDismiss = { if (!saving) showAddDialog = false },
            onSave = { title, brand, equip, symptom, root, res, code, tag ->
                viewModel.addTroubleshootingLog(
                    title = title,
                    brand = brand,
                    equipmentType = equip,
                    symptom = symptom,
                    rootCause = root,
                    resolution = res,
                    errorCode = code,
                    customerTag = tag,
                    onSaved = { showAddDialog = false }
                )
            }
        )
    }
}

@Composable
private fun TroubleshootingLogCard(
    log: TroubleshootingLog,
    onCardClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_card_${log.id}")
            .clickable { onCardClick() },
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
                        color = FrostCyanLight.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = log.brand,
                            color = FrostCyanLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (log.errorCode.isNotBlank()) {
                        Surface(
                            color = FlameOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = log.errorCode,
                                color = FlameOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (log.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favorite",
                            tint = if (log.isFavorite) FrostCyanLight else Slate700
                        )
                    }
                    if (!log.isPresetCheatSheet) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = AlertRed.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.symptom,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Fix: ${log.resolution}",
                style = MaterialTheme.typography.bodySmall,
                color = GaugeGreen,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun AddLogDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var equipType by remember { mutableStateOf("Split System") }
    var symptom by rememberSaveable { mutableStateOf("") }
    var rootCause by rememberSaveable { mutableStateOf("") }
    var resolution by rememberSaveable { mutableStateOf("") }
    var errorCode by rememberSaveable { mutableStateOf("") }
    var customerTag by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New service note",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(200) },
                    label = { Text("Title *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it.take(100) },
                        label = { Text("Brand (Carrier, Goodman...)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = errorCode,
                        onValueChange = { errorCode = it.take(100) },
                        label = { Text("Code / Blinks") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = symptom,
                    onValueChange = { symptom = it.take(4000) },
                    label = { Text("Symptom *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rootCause,
                    onValueChange = { rootCause = it.take(4000) },
                    label = { Text("Root Cause Discovered") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = resolution,
                    onValueChange = { resolution = it.take(8000) },
                    label = { Text("Resolution / work performed *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customerTag,
                    onValueChange = { customerTag = it.take(300) },
                    label = { Text("Site / Tag (e.g. 104 Main St)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && title.isNotBlank() && symptom.isNotBlank() && resolution.isNotBlank(),
                onClick = {
                    if (title.isNotBlank() && symptom.isNotBlank() && resolution.isNotBlank()) {
                        onSave(title, brand, equipType, symptom, rootCause, resolution, errorCode, customerTag)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FrostCyanLight, contentColor = Slate900)
            ) {
                Text(if (saving) "Saving…" else "Save log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = Slate900
    )
}
