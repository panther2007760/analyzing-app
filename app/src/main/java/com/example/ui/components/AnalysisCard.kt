package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AIAnalysis
import com.example.model.AnalysisType
import com.example.model.MarketTrend
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
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalysisCard(
    analysis: AIAnalysis,
    modifier: Modifier = Modifier
) {
    val isBullish = analysis.trend == MarketTrend.BULLISH
    val isBearish = analysis.trend == MarketTrend.BEARISH
    val trendColor = when {
        isBullish -> BullishGreen
        isBearish -> BearishRed
        else -> CyanPrimary
    }
    val trendBg = when {
        isBullish -> BullishGreenSoft
        isBearish -> BearishRedSoft
        else -> Color(0xFF0F2636)
    }

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
            // Header Row: Type Badge + Trend Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Analysis Type Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (analysis.analysisType == AnalysisType.LOCAL_SCREENSHOT) Color(0xFF231C3D) else Color(0xFF0F2636))
                        .border(
                            0.5.dp,
                            if (analysis.analysisType == AnalysisType.LOCAL_SCREENSHOT) Color(0xFFB388FF) else CyanPrimary,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (analysis.analysisType == AnalysisType.LOCAL_SCREENSHOT) Color(0xFFD1B3FF) else CyanPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (analysis.analysisType == AnalysisType.LOCAL_SCREENSHOT) {
                                "LOCAL SCREENSHOT ANALYSIS"
                            } else {
                                "LIVE MARKET DATA ANALYSIS"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = if (analysis.analysisType == AnalysisType.LOCAL_SCREENSHOT) Color(0xFFE2D1FF) else CyanPrimary
                            )
                        )
                    }
                }

                // Trend Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(trendBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${analysis.trend.name} (${analysis.confidencePercent}%)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = trendColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = analysis.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            if (analysis.isDemo) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "[DEMO MODE - PRE-RECORDED SAMPLE]",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DemoAmber,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Identified Patterns FlowRow
            if (analysis.keyPatterns.isNotEmpty()) {
                Text(
                    text = "IDENTIFIED PATTERNS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    analysis.keyPatterns.forEach { pattern ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDark)
                                .border(0.5.dp, BorderDark, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = pattern,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Support & Resistance Levels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Support Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(0.5.dp, BullishGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "SUPPORT LEVELS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BullishGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val supText = if (analysis.supportLevels.isNotEmpty()) {
                            analysis.supportLevels.joinToString(" • ") { "$%,.2f".format(Locale.US, it) }
                        } else "Dynamic trendline shelf"
                        Text(
                            text = supText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Resistance Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(0.5.dp, BearishRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "RESISTANCE LEVELS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BearishRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val resText = if (analysis.resistanceLevels.isNotEmpty()) {
                            analysis.resistanceLevels.joinToString(" • ") { "$%,.2f".format(Locale.US, it) }
                        } else "Overhead supply zone"
                        Text(
                            text = resText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Technical Verdict
            Text(
                text = "TECHNICAL VERDICT",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = analysis.technicalVerdict,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Indicators Summary
            Text(
                text = "MOMENTUM & VOLUME PROFILE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextMuted,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = analysis.indicatorsSummary,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Risk Assessment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2538))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = analysis.riskAssessment,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Non-trading educational disclaimer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = analysis.disclaimer,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
