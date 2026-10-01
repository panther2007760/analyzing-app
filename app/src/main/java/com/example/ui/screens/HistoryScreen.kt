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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MarketRepository
import com.example.data.db.SignalEntity
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(repository: MarketRepository) {
    val language by repository.appLanguage.collectAsStateWithLifecycle()
    val savedSignals by repository.savedSignalsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedSignal by remember { mutableStateOf<SignalEntity?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen")
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
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("history", language).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )
                    }
                    Text(
                        text = "Persisted Local Database (Room)",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                if (savedSignals.isNotEmpty()) {
                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear All", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (savedSignals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No saved signals in database.\nUse the 'Save Signal' button on the Signals tab to store setups.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            items(savedSignals, key = { it.id }) { item ->
                SavedSignalCard(
                    item = item,
                    onClick = { selectedSignal = item },
                    onDelete = { repository.deleteSignal(item.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = SurfaceDark,
            title = { Text("Clear All Signals?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("This will permanently remove all saved signals from your local Room database.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        repository.clearAllSavedSignals()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed)
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    selectedSignal?.let { sig ->
        ModalBottomSheet(
            onDismissRequest = { selectedSignal = null },
            containerColor = SurfaceDark
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "${sig.pairSymbol} - Saved Setup",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(sig.timestamp))
                Text(text = "Saved on: $dateStr", fontSize = 11.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "CONSENSUS & REASONS:", fontWeight = FontWeight.Bold, color = CyanPrimary, fontSize = 12.sp)
                Text(text = sig.reasonsSummary, fontSize = 12.sp, color = TextPrimary)

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "INVALIDATION CONDITION:", fontWeight = FontWeight.Bold, color = BearishRed, fontSize = 12.sp)
                Text(text = sig.invalidation, fontSize = 12.sp, color = TextSecondary)

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        repository.deleteSignal(sig.id)
                        selectedSignal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete from History", color = BearishRed, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SavedSignalCard(item: SignalEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val biasColor = when (item.bias) {
        "LONG_BIAS" -> BullishGreen
        "SHORT_BIAS" -> BearishRed
        else -> Color(0xFFFFB300)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.pairSymbol, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(biasColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.bias.replace("_", " "),
                            color = biasColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Entry: ${item.entryZone}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SL: $${String.format(Locale.US, "%.2f", item.stopLoss)}",
                    fontSize = 11.sp,
                    color = BearishRed,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "TP1: $${String.format(Locale.US, "%.2f", item.takeProfit1)}",
                    fontSize = 11.sp,
                    color = BullishGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
