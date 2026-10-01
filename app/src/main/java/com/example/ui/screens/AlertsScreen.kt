package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MarketRepository
import com.example.data.db.PriceAlertEntity
import com.example.model.AppStrings
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertsScreen(repository: MarketRepository) {
    val language by repository.appLanguage.collectAsStateWithLifecycle()
    val allAlerts by repository.allAlertsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedPair by repository.selectedPair.collectAsStateWithLifecycle()
    val currentTicker by repository.currentTicker.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var notificationFeedback by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { mutableStateOf(repository.alertNotificationManager.hasNotificationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        notificationFeedback = if (granted) "Notification permission granted!" else "Notification permission was denied."
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("alerts_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("price_alerts", language).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )
                    }
                    Text(
                        text = "Real-time background alerts when targets are breached",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_create_alert")
                ) {
                    Icon(imageVector = Icons.Default.AddAlert, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD ALERT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Notification Permission & Test Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasPermission) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (hasPermission) BullishGreen else BearishRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasPermission) "Push Notifications Active" else "Push Notifications Disabled",
                                fontWeight = FontWeight.Bold,
                                color = if (hasPermission) BullishGreen else BearishRed,
                                fontSize = 13.sp
                            )
                        }

                        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Grant Permission", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val sent = repository.testNotification()
                                    notificationFeedback = if (sent) "Test notification dispatched to status bar!" else "Permission missing."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_test_notification")
                            ) {
                                Text("Test Alert", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    notificationFeedback?.let { fb ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = fb, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Active Alerts List
        item {
            Text(
                text = "CONFIGURED PRICE ALERTS (${allAlerts.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
        }

        if (allAlerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No price alerts configured. Tap 'ADD ALERT' to set target levels.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }
        } else {
            items(allAlerts, key = { it.id }) { alert ->
                AlertItemCard(
                    alert = alert,
                    onDelete = { repository.deletePriceAlert(alert.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    if (showAddDialog) {
        AddPriceAlertDialog(
            currentPair = selectedPair.coinbaseProduct,
            currentPrice = currentTicker?.lastPrice ?: 50000.0,
            onDismiss = { showAddDialog = false },
            onConfirm = { pair, price, isAbove ->
                repository.createPriceAlert(pair, price, isAbove)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AlertItemCard(alert: PriceAlertEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = alert.pairSymbol, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (alert.isActive) CyanPrimary.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (alert.isActive) "ACTIVE" else "TRIGGERED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alert.isActive) CyanPrimary else TextMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val cond = if (alert.isAbove) "crosses above" else "drops below"
                Text(
                    text = "Notify when price $cond $${String.format(Locale.US, "%.2f", alert.targetPrice)}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
            }
        }
    }
}

@Composable
fun AddPriceAlertDialog(
    currentPair: String,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Boolean) -> Unit
) {
    var priceText by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentPrice * 1.02)) }
    var isAbove by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Create Price Alert", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column {
                Text(text = "Asset: $currentPair (Current: $${String.format(Locale.US, "%.2f", currentPrice)})", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Target Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = SurfaceCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("input_alert_price")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isAbove,
                        onClick = { isAbove = true },
                        colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                    )
                    Text("When price crosses ABOVE target", fontSize = 12.sp, color = TextPrimary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isAbove,
                        onClick = { isAbove = false },
                        colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                    )
                    Text("When price drops BELOW target", fontSize = 12.sp, color = TextPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    if (p > 0.0) {
                        onConfirm(currentPair, p, isAbove)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Set Alert", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
