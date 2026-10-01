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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MarketRepository
import com.example.model.AppStrings
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun RiskCalculatorScreen(
    repository: MarketRepository,
    initialEntry: Double? = null,
    initialStopLoss: Double? = null
) {
    val language by repository.appLanguage.collectAsStateWithLifecycle()
    val currentTicker by repository.currentTicker.collectAsStateWithLifecycle()
    val selectedPair by repository.selectedPair.collectAsStateWithLifecycle()

    val defaultPrice = initialEntry ?: currentTicker?.lastPrice ?: 60000.0
    val defaultSL = initialStopLoss ?: (defaultPrice * 0.98)

    var balanceText by remember { mutableStateOf("10000") }
    var riskPercentText by remember { mutableStateOf("1.5") }
    var entryText by remember { mutableStateOf(String.format(Locale.US, "%.2f", defaultPrice)) }
    var stopLossText by remember { mutableStateOf(String.format(Locale.US, "%.2f", defaultSL)) }
    var targetText by remember { mutableStateOf(String.format(Locale.US, "%.2f", defaultPrice * 1.03)) }

    val balance = balanceText.toDoubleOrNull() ?: 0.0
    val riskPercent = riskPercentText.toDoubleOrNull() ?: 0.0
    val entryPrice = entryText.toDoubleOrNull() ?: 0.0
    val stopLossPrice = stopLossText.toDoubleOrNull() ?: 0.0
    val targetPrice = targetText.toDoubleOrNull() ?: 0.0

    // Calculations
    val dollarRisk = balance * (riskPercent / 100.0)
    val priceDiff = abs(entryPrice - stopLossPrice)
    val positionSizeUnits = if (priceDiff > 0.0) dollarRisk / priceDiff else 0.0
    val totalPositionValue = positionSizeUnits * entryPrice
    val recommendedLeverage = if (balance > 0.0) totalPositionValue / balance else 1.0

    val potentialProfit = if (entryPrice > 0.0) positionSizeUnits * abs(targetPrice - entryPrice) else 0.0
    val riskRewardRatio = if (dollarRisk > 0.0) potentialProfit / dollarRisk else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("risk_calculator_screen")
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("risk_calculator", language).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    )
                }
                Text(
                    text = "Strict Capital Preservation & Position Sizing",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Button(
                onClick = {
                    currentTicker?.lastPrice?.let { p ->
                        entryText = String.format(Locale.US, "%.2f", p)
                        stopLossText = String.format(Locale.US, "%.2f", p * 0.98)
                        targetText = String.format(Locale.US, "%.2f", p * 1.03)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Use Live Price", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calculation Results Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "CALCULATED POSITION SIZING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CalcStatPill(
                        label = "Dollar Risk ($)",
                        value = String.format(Locale.US, "$%.2f", dollarRisk),
                        color = BearishRed
                    )
                    CalcStatPill(
                        label = "Position Size (${selectedPair.baseAsset})",
                        value = String.format(Locale.US, "%.4f", positionSizeUnits),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CalcStatPill(
                        label = "Total Position ($)",
                        value = String.format(Locale.US, "$%.2f", totalPositionValue),
                        color = TextPrimary
                    )
                    CalcStatPill(
                        label = "Rec. Leverage",
                        value = String.format(Locale.US, "%.1fx", recommendedLeverage),
                        color = if (recommendedLeverage > 5.0) BearishRed else BullishGreen
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CalcStatPill(
                        label = "Target Profit ($)",
                        value = String.format(Locale.US, "$%.2f", potentialProfit),
                        color = BullishGreen
                    )
                    CalcStatPill(
                        label = "Risk / Reward",
                        value = String.format(Locale.US, "1 : %.2f", riskRewardRatio),
                        color = if (riskRewardRatio >= 1.5) BullishGreen else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Fields
        Text(text = "ACCOUNT & RISK PARAMETERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(8.dp))

        CalcTextField(
            label = "Account Balance ($)",
            value = balanceText,
            onValueChange = { balanceText = it },
            tag = "input_balance"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Risk % chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("0.5", "1.0", "1.5", "2.0", "3.0").forEach { r ->
                val selected = riskPercentText == r
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { riskPercentText = r }
                        .weight(1f),
                    color = if (selected) CyanPrimary.copy(alpha = 0.2f) else SurfaceCard,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "$r%",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) CyanPrimary else TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        CalcTextField(
            label = "Risk Percentage (%)",
            value = riskPercentText,
            onValueChange = { riskPercentText = it },
            tag = "input_risk_percent"
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "PRICE LEVELS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(8.dp))

        CalcTextField(
            label = "Entry Price ($)",
            value = entryText,
            onValueChange = { entryText = it },
            tag = "input_entry_price"
        )

        Spacer(modifier = Modifier.height(10.dp))

        CalcTextField(
            label = "Stop Loss Price ($)",
            value = stopLossText,
            onValueChange = { stopLossText = it },
            tag = "input_stop_loss"
        )

        Spacer(modifier = Modifier.height(10.dp))

        CalcTextField(
            label = "Target Price / Take Profit ($)",
            value = targetText,
            onValueChange = { targetText = it },
            tag = "input_target_price"
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun CalcStatPill(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CalcTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanPrimary,
            unfocusedBorderColor = SurfaceCard,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    )
}
