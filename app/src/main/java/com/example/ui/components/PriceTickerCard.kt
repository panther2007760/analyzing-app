package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CryptoPair
import com.example.model.Ticker
import com.example.model.WebSocketConnectionState
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedSoft
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenSoft
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoAmberSoft
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PriceTickerCard(
    pair: CryptoPair,
    ticker: Ticker?,
    webSocketState: WebSocketConnectionState,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    var previousPrice by remember { mutableDoubleStateOf(ticker?.lastPrice ?: 0.0) }
    var priceColorState by remember { mutableStateOf(TextPrimary) }

    LaunchedEffect(ticker?.lastPrice) {
        val current = ticker?.lastPrice ?: 0.0
        if (previousPrice != 0.0 && current != 0.0) {
            priceColorState = if (current >= previousPrice) BullishGreen else BearishRed
            delay(400)
            priceColorState = TextPrimary
        }
        previousPrice = current
    }

    val animatedColor by animateColorAsState(
        targetValue = priceColorState,
        animationSpec = tween(durationMillis = 300),
        label = "price_flash"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Symbol + Display Name + Live WS Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BorderDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pair.iconSymbol,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${pair.baseAsset}/${pair.quoteAsset}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = pair.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }

                // WebSocket Stream Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                !isLive -> DemoAmberSoft
                                webSocketState == WebSocketConnectionState.CONNECTED -> BullishGreenSoft
                                else -> BearishRedSoft
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !isLive -> DemoAmber
                                    webSocketState == WebSocketConnectionState.CONNECTED -> BullishGreen
                                    else -> BearishRed
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            !isLive -> "DEMO STREAM"
                            webSocketState == WebSocketConnectionState.CONNECTED -> "WS REALTIME"
                            webSocketState == WebSocketConnectionState.CONNECTING -> "WS CONNECTING"
                            webSocketState == WebSocketConnectionState.OFFLINE_PAUSED -> "WS PAUSED"
                            else -> "WS RECONNECTING"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            color = when {
                                !isLive -> DemoAmber
                                webSocketState == WebSocketConnectionState.CONNECTED -> BullishGreen
                                else -> BearishRed
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Price + 24h Change Pill
            val isBullish = (ticker?.percentChange24h ?: 0.0) >= 0.0
            val changeSign = if (isBullish) "+" else ""

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "LAST PRICE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = ticker?.let { "$%,.2f".format(Locale.US, it.lastPrice) } ?: "---",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = animatedColor
                        )
                    )
                }

                // 24h Change Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBullish) BullishGreenSoft else BearishRedSoft)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isBullish) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (isBullish) BullishGreen else BearishRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ticker?.let {
                                "%s%.2f%%".format(Locale.US, changeSign, it.percentChange24h)
                            } ?: "0.00%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isBullish) BullishGreen else BearishRed
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row: 24h High, 24h Low, 24h Volume
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "24H HIGH",
                    value = ticker?.let { "$%,.2f".format(Locale.US, it.highPrice24h) } ?: "---"
                )
                StatItem(
                    label = "24H LOW",
                    value = ticker?.let { "$%,.2f".format(Locale.US, it.lowPrice24h) } ?: "---"
                )
                StatItem(
                    label = "24H VOL",
                    value = ticker?.let { "%,.1f".format(Locale.US, it.volume24h) } ?: "---"
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        )
    }
}
