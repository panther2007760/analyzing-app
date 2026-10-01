package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.CryptoPair
import com.example.model.MarketDataStatus
import com.example.model.Timeframe
import com.example.ui.components.AnalysisCard
import com.example.ui.components.CandlestickChart
import com.example.ui.components.PriceTickerCard
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedSoft
import com.example.ui.theme.BorderDark
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
fun MarketScreen(
    repository: MarketRepository,
    selectedPair: CryptoPair,
    ticker: com.example.model.Ticker?,
    candles: List<com.example.model.CandleStick>,
    webSocketState: com.example.model.WebSocketConnectionState,
    isLive: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    aiAnalysis: com.example.model.AIAnalysis?,
    isAiLoading: Boolean,
    onSelectPair: (CryptoPair) -> Unit,
    onRefresh: () -> Unit,
    onRunAiAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val currentTimeframe by repository.currentTimeframe.collectAsStateWithLifecycle()
    val marketDataStatus by repository.marketDataStatus.collectAsStateWithLifecycle()
    val lastUpdateTime by repository.lastDataUpdateTime.collectAsStateWithLifecycle()
    val lastAnalysisTime by repository.lastAnalysisTime.collectAsStateWithLifecycle()
    val currentSignal by repository.currentSignal.collectAsStateWithLifecycle()

    var showDiagnosticPanel by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("market_screen")
    ) {
        // Pair Selector horizontal scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CryptoPair.DEFAULT_PAIRS.forEach { pair ->
                val isSelected = pair.symbol == selectedPair.symbol
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) CyanPrimary else SurfaceCard)
                        .border(
                            1.dp,
                            if (isSelected) CyanPrimary else BorderDark,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectPair(pair) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = pair.iconSymbol,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else CyanPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pair.baseAsset,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Pipeline Status Bar
        MarketStatusBanner(
            status = marketDataStatus,
            lastUpdateTime = lastUpdateTime,
            currentPrice = ticker?.lastPrice ?: candles.lastOrNull()?.close ?: 0.0,
            onRefresh = onRefresh
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Price Ticker Card
        PriceTickerCard(
            pair = selectedPair,
            ticker = ticker,
            webSocketState = webSocketState,
            isLive = isLive
        )

        // Error Notice if API failed
        AnimatedVisibility(visible = errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BearishRedSoft)
                    .border(1.dp, BearishRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = BearishRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chart Header with Refresh & Timeframe
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${currentTimeframe.label.uppercase()} CANDLESTICK CHART",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = CyanPrimary
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Chart",
                    tint = CyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Candlestick Chart (Canvas) with EMA and SR overlays
        CandlestickChart(
            candles = candles,
            selectedTimeframe = currentTimeframe,
            onSelectTimeframe = { tf -> repository.setTimeframe(tf) },
            supportLevel = currentSignal?.supportLevels?.firstOrNull(),
            resistanceLevel = currentSignal?.resistanceLevels?.firstOrNull(),
            currentPrice = ticker?.lastPrice
        )

        Spacer(modifier = Modifier.height(16.dp))

        // DEVELOPER / TECHNICAL ANALYSIS DIAGNOSTIC PANEL (Section 22 of prompt)
        currentSignal?.let { sig ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("technical_analysis_panel"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIVE TECHNICAL ENGINE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        // Consensus Bias Pill
                        val biasColor = when (sig.bias) {
                            SignalBias.LONG_BIAS -> BullishGreen
                            SignalBias.SHORT_BIAS -> BearishRed
                            SignalBias.WAIT -> Color(0xFFFFB300)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(biasColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${sig.bias.name.replace("_", " ")} (${sig.confidencePercent}%)",
                                color = biasColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Core Metrics Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TechMetricCol("RSI 14", "%.1f".format(Locale.US, sig.rsi))
                        TechMetricCol("EMA 9", formatPrice(sig.ema9))
                        TechMetricCol("EMA 21", formatPrice(sig.ema21))
                        TechMetricCol("EMA 50", formatPrice(sig.ema50))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TechMetricCol("MACD Line", "%.4f".format(Locale.US, sig.macdLine))
                        TechMetricCol("Signal Line", "%.4f".format(Locale.US, sig.macdSignal))
                        TechMetricCol("Histogram", "%+.4f".format(Locale.US, sig.macdHistogram))
                        TechMetricCol("ATR (14)", formatPrice(sig.atr))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TechMetricCol("BB Upper", formatPrice(sig.bollingerBands.upper))
                        TechMetricCol("BB Middle", formatPrice(sig.bollingerBands.middle))
                        TechMetricCol("BB Lower", formatPrice(sig.bollingerBands.lower))
                        TechMetricCol("Bandwidth", "%.2f%%".format(Locale.US, sig.bollingerBands.bandwidth))
                    }

                    HorizontalDivider(
                        color = TextMuted.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Market Structure & Candle Pattern
                    Text(
                        text = "MARKET STRUCTURE & PATTERN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Structure: ${sig.marketStructure.structureSummary}",
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Last Closed Candle Pattern: ${sig.candlestickPattern.description}",
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Multi-Timeframe: HTF: ${sig.htfTrend} | CTF: ${sig.ctfTrend} | LTF: ${sig.ltfMomentum}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    HorizontalDivider(
                        color = TextMuted.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Trade Invalidation & Timing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val lastClosedStr = remember(sig.lastClosedCandleTime) {
                            if (sig.lastClosedCandleTime > 0) {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sig.lastClosedCandleTime))
                            } else "---"
                        }
                        val lastAnalysisStr = remember(sig.timestamp) {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sig.timestamp))
                        }

                        Text(text = "Closed Candle: $lastClosedStr", fontSize = 10.sp, color = TextMuted)
                        Text(text = "Calculated at: $lastAnalysisStr", fontSize = 10.sp, color = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Invalidation: ${sig.invalidationCondition}",
                        fontSize = 11.sp,
                        color = BearishRed,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Button: Run AI Technical Explanation
        Button(
            onClick = onRunAiAnalysis,
            enabled = !isAiLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("run_ai_analysis_button")
        ) {
            if (isAiLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Synthesizing Live Technical Data...",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI TECHNICAL EXPLANATION",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Analysis Display Card
        if (aiAnalysis != null) {
            AnalysisCard(
                analysis = aiAnalysis,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap 'AI TECHNICAL EXPLANATION' to generate an educational narrative synthesizing the live mathematical calculations above.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun MarketStatusBanner(
    status: MarketDataStatus,
    lastUpdateTime: Long,
    currentPrice: Double,
    onRefresh: () -> Unit
) {
    val (statusLabel, statusColor) = when (status) {
        MarketDataStatus.LIVE -> "LIVE" to BullishGreen
        MarketDataStatus.CONNECTING -> "CONNECTING" to Color(0xFFFFB300)
        MarketDataStatus.STALE -> "STALE DATA" to Color(0xFFFF9800)
        MarketDataStatus.OFFLINE -> "OFFLINE" to BearishRed
        MarketDataStatus.ERROR -> "DATA ERROR" to BearishRed
    }

    val timeFormatted = remember(lastUpdateTime) {
        if (lastUpdateTime > 0L) {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastUpdateTime))
        } else "Connecting..."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusLabel,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = statusColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Tick: $timeFormatted",
                    fontSize = 10.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = formatPrice(currentPrice),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun TechMetricCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatPrice(v: Double): String {
    return if (v >= 100.0) String.format(Locale.US, "$%.2f", v)
    else if (v >= 1.0) String.format(Locale.US, "$%.4f", v)
    else String.format(Locale.US, "$%.6f", v)
}
