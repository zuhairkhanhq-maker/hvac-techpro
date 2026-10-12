package com.example.ui.screens

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DiagnosticMessage
import com.example.data.model.DiagnosticScenarios
import com.example.data.model.MessageSender
import com.example.data.model.UserTier
import com.example.ui.HvacViewModel
import com.example.ui.components.LockedFeatureGate
import com.example.ui.components.TierBadge
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FrostCyanLight
import com.example.ui.theme.GaugeGreen
import com.example.ui.theme.HvacBlue
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun SeniorTechScreen(
    viewModel: HvacViewModel,
    onOpenSubscriptionModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTier by viewModel.currentTier.collectAsState()
    val isUnlocked = currentTier.canAccessSeniorTechAI

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
                    requiredTier = UserTier.PLUS_USER,
                    featureName = "Offline field guide",
                    valueProp = "Find keyword-selected reference checklists. This is not conversational AI or a diagnosis.",
                    onUpgradeClick = onOpenSubscriptionModal
                )
            }
        } else {
            SeniorTechChatInterface(
                viewModel = viewModel,
                currentTier = currentTier
            )
        }
    }
}

@Composable
private fun SeniorTechChatInterface(
    viewModel: HvacViewModel,
    currentTier: UserTier
) {
    val messages by viewModel.diagnosticMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val listState = rememberLazyListState()

    var inputPrompt by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Top Header
        Surface(
            color = Slate900,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(FlameOrange.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Engineering,
                            contentDescription = null,
                            tint = FlameOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Offline field guide",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (currentTier == UserTier.PRO_USER) "Keyword reference · Pro" else "Keyword reference · Plus",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierBadge(tier = currentTier)
                    IconButton(onClick = { viewModel.clearDiagnosticHistory() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Chat",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Quick Starter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DiagnosticScenarios.fieldScenarios) { scenario ->
                FilterChip(
                    selected = false,
                    onClick = {
                        viewModel.sendDiagnosticQuery(scenario.initialPrompt)
                    },
                    label = { Text(scenario.title, fontSize = 11.5.sp) }
                )
            }
        }

        // Chat Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                DiagnosticMessageBubble(
                    message = message,
                    onQuickActionClick = { viewModel.sendDiagnosticQuery(it) }
                )
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = FrostCyanLight,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = "Finding a reference checklist…",
                            style = MaterialTheme.typography.bodySmall,
                            color = FrostCyanLight
                        )
                    }
                }
            }
        }

        // Input Bar
        Surface(
            color = Slate900,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { inputPrompt = it.take(4000) },
                    placeholder = { Text("Ask about pressures, blinks, compressor trips...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("senior_tech_input"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FrostCyanLight,
                        unfocusedBorderColor = Slate700
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (inputPrompt.isNotBlank()) {
                            viewModel.sendDiagnosticQuery(inputPrompt)
                            inputPrompt = ""
                        }
                    },
                    enabled = inputPrompt.isNotBlank() && !isThinking,
                    modifier = Modifier.testTag("senior_tech_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Query",
                        tint = if (inputPrompt.isNotBlank() && !isThinking) FrostCyanLight else Slate700
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticMessageBubble(
    message: DiagnosticMessage,
    onQuickActionClick: (String) -> Unit
) {
    val isUser = message.sender == MessageSender.TECHNICIAN

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(FlameOrange.copy(alpha = 0.2f), CircleShape)
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = null,
                    tint = FlameOrange,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = if (isUser) FrostCyanLight.copy(alpha = 0.2f) else Slate800,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) FrostCyanLight.copy(alpha = 0.4f) else Slate700
                )
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    lineHeight = 21.sp
                )
            }

            if (message.quickActions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    message.quickActions.forEach { action ->
                        Surface(
                            color = Slate900,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FrostCyanLight.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .testTag("quick_action_${action.take(10).replace(" ", "_")}")
                                .background(Color.Transparent)
                        ) {
                            Text(
                                text = "👉 $action",
                                color = FrostCyanLight,
                                fontSize = 11.5.sp,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clickable { onQuickActionClick(action) }
                            )
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(HvacBlue.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = FrostCyanLight,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
