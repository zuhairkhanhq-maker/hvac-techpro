package com.example.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.UserTier
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionBottomSheet(currentTier: UserTier, onSelectTier: (UserTier) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding().testTag("plan_list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Your plan", style = MaterialTheme.typography.headlineSmall)
                Text(if (BuildConfig.DEBUG) "Test build: preview tiers. No purchase is created." else "Free tools are available. Paid activation requires the Play Billing release configuration.")
            }
            items(listOf(UserTier.REGULAR_USER, UserTier.PLUS_USER, UserTier.PRO_USER)) { tier ->
                val active = currentTier == tier || (tier == UserTier.REGULAR_USER && currentTier == UserTier.NEW_USER)
                Card(Modifier.fillMaxWidth().testTag("tier_plan_${tier.name.lowercase()}")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(tier.title, style = MaterialTheme.typography.titleLarge)
                        Text(if (tier == UserTier.REGULAR_USER) "Free" else tier.priceDescription)
                        Text(when (tier) {
                            UserTier.PLUS_USER -> "Unlimited equipment and jobs, nameplate OCR, full history, guided workflows and offline PDF reports."
                            UserTier.PRO_USER -> "Everything in Plus, branded photo reports, approvals, reusable inspections, maintenance reminders, encrypted recovery and a private business dashboard."
                            else -> "Field calculators, PT reference, 3 equipment records and 10 completed jobs."
                        })
                        Button(enabled = !active && (BuildConfig.DEBUG || tier == UserTier.REGULAR_USER), onClick = { onSelectTier(tier); onDismiss() }, modifier = Modifier.fillMaxWidth().testTag("select_tier_${tier.name.lowercase()}")) {
                            Text(if (active) "Current plan" else if (BuildConfig.DEBUG) "Preview ${tier.title}" else "Not available yet")
                        }
                    }
                }
            }
            item { TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") } }
        }
    }
}
