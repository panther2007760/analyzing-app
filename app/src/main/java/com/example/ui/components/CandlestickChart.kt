package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.IndicatorCalculator
import com.example.model.CandleStick
import com.example.model.Timeframe
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.ChartCrosshair
import com.example.ui.theme.ChartGridLine
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChart(
    candles: List<CandleStick>,
    selectedTimeframe: Timeframe = Timeframe.H1,
    onSelectTimeframe: (Timeframe) -> Unit = {},
    supportLevel: Double? = null,
    resistanceLevel: Double? = null,
    currentPrice: Double? = null,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(290.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Connecting to real-time market candlestick feed...",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
            )
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showEMA by remember { mutableStateOf(true) }
    var showSR by remember { mutableStateOf(true) }

    val displayCandle = selectedIndex?.let { candles.getOrNull(it) } ?: candles.lastOrNull()

    val closes = remember(candles) { candles.map { it.close } }
    val ema9List = remember(candles) { IndicatorCalculator.calculateEMA(closes, 9) }
    val ema21List = remember(candles) { IndicatorCalculator.calculateEMA(closes, 21) }

    val minPrice = candles.minOf { it.low }
    val maxPrice = candles.maxOf { it.high }
    val priceRange = max(maxPrice - minPrice, 0.0001)
    val maxVolume = candles.maxOfOrNull { it.volume } ?: 1.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("candlestick_chart_container")
    ) {
        // Timeframe selector & Overlay Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(Timeframe.entries) { tf ->
                    val isSelected = tf == selectedTimeframe
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectTimeframe(tf) },
                        color = if (isSelected) CyanPrimary else SurfaceCard,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tf.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showEMA = !showEMA },
                    color = if (showEMA) CyanPrimary.copy(alpha = 0.2f) else SurfaceCard
                ) {
                    Text(
                        text = "EMA 9/21",
                        fontSize = 10.sp,
                        color = if (showEMA) CyanPrimary else TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showSR = !showSR },
                    color = if (showSR) Color(0xFFFF9800).copy(alpha = 0.2f) else SurfaceCard
                ) {
                    Text(
                        text = "S / R",
                        fontSize = 10.sp,
                        color = if (showSR) Color(0xFFFF9800) else TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Top HUD Info Bar
        if (displayCandle != null) {
            val isBullish = displayCandle.close >= displayCandle.open
            val candleChange = if (displayCandle.open > 0) ((displayCandle.close - displayCandle.open) / displayCandle.open) * 100.0 else 0.0
            val color = if (isBullish) BullishGreen else BearishRed

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeStr = remember(displayCandle.openTime) {
                    SimpleDateFormat("HH:mm MM/dd", Locale.getDefault()).format(Date(displayCandle.openTime))
                }

                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "O: ${formatVal(displayCandle.open)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "H: ${formatVal(displayCandle.high)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "L: ${formatVal(displayCandle.low)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "C: ${formatVal(displayCandle.close)} (${String.format(Locale.US, "%+.2f%%", candleChange)})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
        }

        // Main Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(candles) {
                        detectTapGestures(
                            onTap = { offset ->
                                val candleWidth = size.width / candles.size
                                val idx = (offset.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                selectedIndex = idx
                            }
                        )
                    }
                    .pointerInput(candles) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val candleWidth = size.width / candles.size
                                val idx = (offset.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                selectedIndex = idx
                            },
                            onDrag = { change, _ ->
                                val candleWidth = size.width / candles.size
                                val idx = (change.position.x / candleWidth).toInt().coerceIn(0, candles.size - 1)
                                selectedIndex = idx
                            },
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null }
                        )
                    }
            ) {
                val chartHeight = size.height * 0.78f
                val volumeHeight = size.height * 0.20f
                val chartTop = 10f
                val candleCount = candles.size
                val candleSlotWidth = size.width / candleCount
                val candleBodyWidth = max(candleSlotWidth * 0.65f, 2f)

                // 1. Grid lines
                val gridSteps = 4
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (i in 0..gridSteps) {
                    val y = chartTop + (chartHeight / gridSteps) * i
                    drawLine(
                        color = ChartGridLine,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                // 2. Support & Resistance Overlay
                if (showSR) {
                    resistanceLevel?.let { res ->
                        if (res in minPrice..maxPrice) {
                            val resY = chartTop + ((maxPrice - res) / priceRange * chartHeight).toFloat()
                            drawLine(
                                color = BearishRed.copy(alpha = 0.75f),
                                start = Offset(0f, resY),
                                end = Offset(size.width, resY),
                                strokeWidth = 1.5f,
                                pathEffect = dashEffect
                            )
                        }
                    }

                    supportLevel?.let { sup ->
                        if (sup in minPrice..maxPrice) {
                            val supY = chartTop + ((maxPrice - sup) / priceRange * chartHeight).toFloat()
                            drawLine(
                                color = BullishGreen.copy(alpha = 0.75f),
                                start = Offset(0f, supY),
                                end = Offset(size.width, supY),
                                strokeWidth = 1.5f,
                                pathEffect = dashEffect
                            )
                        }
                    }
                }

                // 3. Draw Candlesticks & Volume
                candles.forEachIndexed { i, candle ->
                    val xCenter = i * candleSlotWidth + (candleSlotWidth / 2f)

                    val highY = chartTop + ((maxPrice - candle.high) / priceRange * chartHeight).toFloat()
                    val lowY = chartTop + ((maxPrice - candle.low) / priceRange * chartHeight).toFloat()
                    val openY = chartTop + ((maxPrice - candle.open) / priceRange * chartHeight).toFloat()
                    val closeY = chartTop + ((maxPrice - candle.close) / priceRange * chartHeight).toFloat()

                    val isBull = candle.close >= candle.open
                    val color = if (isBull) BullishGreen else BearishRed

                    // Wick
                    drawLine(
                        color = color,
                        start = Offset(xCenter, highY),
                        end = Offset(xCenter, lowY),
                        strokeWidth = 1.5f
                    )

                    // Body
                    val bodyTop = min(openY, closeY)
                    val bodyHeight = max(abs(openY - closeY), 2f)
                    drawRect(
                        color = color,
                        topLeft = Offset(xCenter - (candleBodyWidth / 2f), bodyTop),
                        size = Size(candleBodyWidth, bodyHeight)
                    )

                    // Volume
                    val vRatio = (candle.volume / maxVolume).coerceIn(0.05, 1.0).toFloat()
                    val barHeight = volumeHeight * vRatio
                    val barTop = size.height - barHeight
                    drawRect(
                        color = color.copy(alpha = 0.30f),
                        topLeft = Offset(xCenter - (candleBodyWidth / 2f), barTop),
                        size = Size(candleBodyWidth, barHeight)
                    )
                }

                // 4. EMA 9 Overlay (Cyan)
                if (showEMA && ema9List.isNotEmpty()) {
                    val offset9 = candles.size - ema9List.size
                    for (i in 0 until ema9List.size - 1) {
                        val x1 = (i + offset9) * candleSlotWidth + (candleSlotWidth / 2f)
                        val y1 = chartTop + ((maxPrice - ema9List[i]) / priceRange * chartHeight).toFloat()
                        val x2 = (i + 1 + offset9) * candleSlotWidth + (candleSlotWidth / 2f)
                        val y2 = chartTop + ((maxPrice - ema9List[i + 1]) / priceRange * chartHeight).toFloat()
                        drawLine(
                            color = CyanPrimary,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 2f
                        )
                    }
                }

                // 5. EMA 21 Overlay (Orange)
                if (showEMA && ema21List.isNotEmpty()) {
                    val offset21 = candles.size - ema21List.size
                    for (i in 0 until ema21List.size - 1) {
                        val x1 = (i + offset21) * candleSlotWidth + (candleSlotWidth / 2f)
                        val y1 = chartTop + ((maxPrice - ema21List[i]) / priceRange * chartHeight).toFloat()
                        val x2 = (i + 1 + offset21) * candleSlotWidth + (candleSlotWidth / 2f)
                        val y2 = chartTop + ((maxPrice - ema21List[i + 1]) / priceRange * chartHeight).toFloat()
                        drawLine(
                            color = Color(0xFFFF9800),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 2f
                        )
                    }
                }

                // 6. Current Price Line
                val activePrice = currentPrice ?: candles.lastOrNull()?.close
                activePrice?.let { p ->
                    if (p in minPrice..maxPrice) {
                        val pY = chartTop + ((maxPrice - p) / priceRange * chartHeight).toFloat()
                        drawLine(
                            color = CyanPrimary.copy(alpha = 0.7f),
                            start = Offset(0f, pY),
                            end = Offset(size.width, pY),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }
                }

                // 7. Crosshair
                selectedIndex?.let { idx ->
                    if (idx in candles.indices) {
                        val c = candles[idx]
                        val x = idx * candleSlotWidth + (candleSlotWidth / 2f)
                        val y = chartTop + ((maxPrice - c.close) / priceRange * chartHeight).toFloat()

                        drawLine(
                            color = ChartCrosshair,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                        drawLine(
                            color = ChartCrosshair,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                        drawCircle(
                            color = CyanPrimary,
                            radius = 4f,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // High and Low Price Labels
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 2.dp)
            ) {
                Text(
                    text = formatVal(maxPrice),
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 42.dp)
            ) {
                Text(
                    text = formatVal(minPrice),
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}

private fun formatVal(v: Double): String {
    return if (v >= 100.0) String.format(Locale.US, "%.2f", v)
    else if (v >= 1.0) String.format(Locale.US, "%.4f", v)
    else String.format(Locale.US, "%.6f", v)
}
