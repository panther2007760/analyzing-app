package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MarketRepository
import com.example.model.AppLanguage
import com.example.model.AppStrings
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    repository: MarketRepository,
    onNavigateToAlerts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToForex: () -> Unit,
    onNavigateToRiskCalc: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val language by repository.appLanguage.collectAsStateWithLifecycle()
    val isDarkMode by repository.isDarkMode.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AppStrings.get("settings", language).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            )
        }
        Text(
            text = "Preferences, Tools, Localization & Diagnostics",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Preference Card (Language & Theme)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "PREFERENCES & LOCALIZATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanPrimary, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // Language Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = if (language == AppLanguage.ENGLISH) AppLanguage.SINHALA else AppLanguage.ENGLISH
                            repository.setLanguage(next)
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Language / භාෂාව", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = language.displayName, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Text(
                        text = if (language == AppLanguage.ENGLISH) "Switch to සිංහල" else "Switch to English",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary
                    )
                }

                HorizontalDivider(color = TextMuted.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                // Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Theme Appearance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = if (isDarkMode) "Dark Mode (Default)" else "Light Mode", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { repository.toggleTheme() },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tools & Features
        Text(text = "ANALYTICAL TOOLS & PERSISTENCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                SettingsNavigationRow(
                    icon = Icons.Default.Notifications,
                    title = AppStrings.get("price_alerts", language),
                    subtitle = "Manage background price target alerts",
                    onClick = onNavigateToAlerts
                )
                HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
                SettingsNavigationRow(
                    icon = Icons.Default.History,
                    title = AppStrings.get("history", language),
                    subtitle = "View saved signals in local Room database",
                    onClick = onNavigateToHistory
                )
                HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
                SettingsNavigationRow(
                    icon = Icons.Default.Calculate,
                    title = AppStrings.get("risk_calculator", language),
                    subtitle = "Account risk % and position sizing calculator",
                    onClick = onNavigateToRiskCalc
                )
                HorizontalDivider(color = TextMuted.copy(alpha = 0.15f))
                SettingsNavigationRow(
                    icon = Icons.Default.CurrencyExchange,
                    title = AppStrings.get("forex_rates", language),
                    subtitle = "European Central Bank reference exchange rates",
                    onClick = onNavigateToForex
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostics & Policy
        Text(text = "SYSTEM & DIAGNOSTICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                SettingsNavigationRow(
                    icon = Icons.Default.BugReport,
                    title = "Network & API Diagnostics",
                    subtitle = "Real-time inspection of API endpoints and WebSocket",
                    onClick = onNavigateToDebug
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security & Disclaimer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "LEGAL & SECURITY DISCLAIMER", fontWeight = FontWeight.Bold, color = CyanPrimary, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aivora Chart AI is an algorithmic chart analysis application strictly for informational and educational purposes. It does NOT execute trades, does NOT manage private keys or funds, and does NOT provide financial or investment advice. All financial trading carries substantial risk.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(12.dp)
        )
    }
}
