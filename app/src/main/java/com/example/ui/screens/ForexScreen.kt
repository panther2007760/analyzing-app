package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.forex.ForexPair
import com.example.model.AppStrings
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ForexScreen(repository: MarketRepository) {
    val language by repository.appLanguage.collectAsStateWithLifecycle()
    val forexPairs by repository.forexRepository.pairs.collectAsStateWithLifecycle()
    val providerStatus by repository.forexRepository.providerStatus.collectAsStateWithLifecycle()
    val isLoading by repository.forexRepository.isLoading.collectAsStateWithLifecycle()
    val errorMessage by repository.forexRepository.errorMessage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("forex_screen")
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
                        Icon(imageVector = Icons.Default.CurrencyExchange, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("forex_rates", language).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        )
                    }
                    Text(
                        text = "Major Foreign Exchange Currency Pairs",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                IconButton(
                    onClick = { scope.launch { repository.forexRepository.refreshForexRates() } },
                    enabled = !isLoading,
                    modifier = Modifier.testTag("btn_refresh_forex")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CyanPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanPrimary)
                    }
                }
            }
        }

        // Provider Status Notice (No-fake-data rule)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PROVIDER STATUS",
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = providerStatus,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Daily Reference Rates: European Central Bank via Frankfurter API (Active)\n• Real-Time Tick Streaming: NOT CONFIGURED (Requires OANDA / AlphaVantage / Fixer API integration). No simulated ticks are presented.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        errorMessage?.let { err ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f))
                ) {
                    Text(text = err, color = Color(0xFFFF5252), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                }
            }
        }

        item {
            Text(text = "CURRENCY PAIR QUOTES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }

        items(forexPairs, key = { it.symbol }) { pair ->
            ForexPairCard(pair = pair)
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }
}

@Composable
fun ForexPairCard(pair: ForexPair) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = pair.symbol, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text(
                    text = if (pair.lastUpdate.isNotBlank()) "ECB Reference: ${pair.lastUpdate}" else "Awaiting refresh",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (pair.rate > 0.0) {
                    Text(
                        text = String.format(Locale.US, "%.4f", pair.rate),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Text(text = "---", fontSize = 16.sp, color = TextMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = "DAILY REF", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
