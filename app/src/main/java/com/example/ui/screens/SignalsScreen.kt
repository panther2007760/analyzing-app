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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.engine.SignalBias
import com.example.data.engine.SignalStrength
import com.example.data.engine.TechnicalSignal
import com.example.model.AppStrings
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedSoft
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenSoft
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalsScreen(
    repository: MarketRepository,
    onNavigateToRiskCalc: (Double, Double) -> Unit
) {
    val language by repository.appLanguage.collectAsStateWithLifecycle()
    val currentSignal by repository.currentSignal.collectAsStateWithLifecycle()
    val scannerSignals by repository.scannerSignals.collectAsStateWithLifecycle()
    val isScanning by repository.isScanning.collectAsStateWithLifecycle()
    val selectedPair by repository.selectedPair.collectAsStateWithLifecycle()

    var selectedDetailSignal by remember { mutableStateOf<TechnicalSignal?>(null) }
    var signalFilter by remember { mutableStateOf("ALL") }
    var isCurrentSaved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("signals_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.get("tab_signals", language).uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Algorithmic Consensus • RSI 14, EMA 9/21, MACD",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Button(
                    onClick = { repository.scanAllPairs() },
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("scan_all_button")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Radar, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SCAN ALL", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Active Focus Signal Card
        item {
            currentSignal?.let { sig ->
                Text(
                    text = "ACTIVE PAIR SIGNAL (${selectedPair.displayName})",
                    style = MaterialTheme.typography.labelMedium.copy(color = CyanPrimary, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SignalCard(
                    signal = sig,
                    isSaved = isCurrentSaved,
                    onSaveClick = {
                        repository.saveCurrentSignal()
                        isCurrentSaved = true
                    },
                    onOpenRiskCalc = {
                        onNavigateToRiskCalc(sig.currentPrice, sig.stopLoss)
                    },
                    onCardClick = { selectedDetailSignal = sig }
                )
            } ?: run {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Loading technical metrics for ${selectedPair.displayName}...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        // Scanner Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MULTI-PAIR MARKET SCANNER",
                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val filters = listOf("ALL", "BULLISH", "BEARISH", "WAIT")
                    items(filters) { f ->
                        val selected = signalFilter == f
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { signalFilter = f }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (selected) CyanPrimary.copy(alpha = 0.2f) else SurfaceCard,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = f,
                                color = if (selected) CyanPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        val filteredSignals = scannerSignals.filter { s ->
            when (signalFilter) {
                "BULLISH" -> s.bias == SignalBias.LONG_BIAS
                "BEARISH" -> s.bias == SignalBias.SHORT_BIAS
                "WAIT" -> s.bias == SignalBias.WAIT
                else -> true
            }
        }

        if (scannerSignals.isEmpty() && !isScanning) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Radar, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tap 'SCAN ALL' above to scan all default crypto pairs across EMA 9/21, RSI 14, and MACD simultaneously.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredSignals) { sig ->
                SignalMiniRow(signal = sig, onClick = { selectedDetailSignal = sig })
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    // Detail Modal Sheet
    selectedDetailSignal?.let { sig ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedDetailSignal = null },
            sheetState = sheetState,
            containerColor = SurfaceDark
        ) {
            SignalDetailModalContent(
                signal = sig,
                onSave = {
                    repository.saveCurrentSignal()
                    selectedDetailSignal = null
                },
                onRiskCalc = {
                    selectedDetailSignal = null
                    onNavigateToRiskCalc(sig.currentPrice, sig.stopLoss)
                }
            )
        }
    }
}

@Composable
fun SignalCard(
    signal: TechnicalSignal,
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onOpenRiskCalc: () -> Unit,
    onCardClick: () -> Unit
) {
    val biasColor = when (signal.bias) {
        SignalBias.LONG_BIAS -> BullishGreen
        SignalBias.SHORT_BIAS -> BearishRed
        SignalBias.WAIT -> Color(0xFFFFB300)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(biasColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = signal.bias.name.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = biasColor,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${signal.strength.name.replace("_", " ")}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                IconButton(onClick = onSaveClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Signal",
                        tint = if (isSaved) CyanPrimary else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Indicator metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(label = "RSI (14)", value = String.format(Locale.US, "%.1f", signal.rsi))
                MetricPill(label = "EMA 9", value = formatVal(signal.ema9))
                MetricPill(label = "EMA 21", value = formatVal(signal.ema21))
                MetricPill(label = "MACD Hist", value = String.format(Locale.US, "%.4f", signal.macdHistogram))
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (signal.bias != SignalBias.WAIT) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "ENTRY ZONE", fontSize = 10.sp, color = TextMuted)
                        Text(text = signal.entryZone, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column {
                        Text(text = "STOP LOSS", fontSize = 10.sp, color = BearishRed)
                        Text(text = formatVal(signal.stopLoss), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BearishRed)
                    }
                    Column {
                        Text(text = "TARGET (TP1)", fontSize = 10.sp, color = BullishGreen)
                        Text(text = formatVal(signal.takeProfit1), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFB300).copy(alpha = 0.1f))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (signal.isMixedConflict) "Indicators giving mixed readings. Wait for confirmation." else "Market consolidating. Breakout required.",
                            fontSize = 12.sp,
                            color = Color(0xFFFFB300)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenRiskCalc,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Risk Calculator", fontSize = 11.sp, color = CyanPrimary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onCardClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Full Reasons", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SignalMiniRow(signal: TechnicalSignal, onClick: () -> Unit) {
    val biasColor = when (signal.bias) {
        SignalBias.LONG_BIAS -> BullishGreen
        SignalBias.SHORT_BIAS -> BearishRed
        SignalBias.WAIT -> Color(0xFFFFB300)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = signal.pairSymbol, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Text(text = "RSI ${String.format(Locale.US, "%.1f", signal.rsi)} • Price ${formatVal(signal.currentPrice)}", fontSize = 11.sp, color = TextSecondary)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(biasColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = signal.bias.name.replace("_", " "),
                    color = biasColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun MetricPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SignalDetailModalContent(
    signal: TechnicalSignal,
    onSave: () -> Unit,
    onRiskCalc: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "${signal.pairSymbol} - SIGNAL BREAKDOWN",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        )
        Text(
            text = "Generated from 1-hour Candlestick Consensus",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "TECHNICAL REASONS & CONSENSUS:", fontWeight = FontWeight.Bold, color = CyanPrimary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        signal.reasons.forEach { r ->
            Text(text = "• $r", fontSize = 12.sp, color = TextPrimary, modifier = Modifier.padding(vertical = 2.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "INVALIDATION CONDITION:", fontWeight = FontWeight.Bold, color = BearishRed, fontSize = 12.sp)
        Text(text = signal.invalidationCondition, fontSize = 12.sp, color = TextSecondary)

        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save to History", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onRiskCalc,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Open Calculator", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun formatVal(v: Double): String {
    return if (v >= 100.0) String.format(Locale.US, "$%.2f", v)
    else if (v >= 1.0) String.format(Locale.US, "$%.4f", v)
    else String.format(Locale.US, "$%.6f", v)
}
