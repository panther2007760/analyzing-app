package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DebugMetrics
import com.example.model.NetworkStatus
import com.example.model.WebSocketConnectionState
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedSoft
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenSoft
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
fun DebugScreen(
    networkStatus: NetworkStatus,
    metrics: DebugMetrics,
    onRetryConnection: () -> Unit,
    onTestApi: () -> Unit,
    onTestWebSocket: () -> Unit,
    onTestAi: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "NETWORK & API DIAGNOSTICS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
            )
        }
        Text(
            text = "Real-time inspection of connectivity interfaces, REST endpoints, WebSocket streams, and Gemini AI connectivity.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic Cards Grid
        DiagnosticSectionCard(title = "1. INTERNET CONNECTIVITY") {
            DiagRow(
                icon = if (networkStatus.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                iconColor = if (networkStatus.isConnected) BullishGreen else BearishRed,
                label = "Internet Status",
                value = if (networkStatus.isConnected) "CONNECTED" else "DISCONNECTED",
                valueColor = if (networkStatus.isConnected) BullishGreen else BearishRed
            )
            DiagRow(
                icon = Icons.Default.Lan,
                iconColor = CyanPrimary,
                label = "Network Type",
                value = networkStatus.networkType.name,
                subtext = networkStatus.interfaceDetails
            )
            DiagRow(
                icon = Icons.Default.Dns,
                iconColor = CyanPrimary,
                label = "Metered / Wi-Fi",
                value = if (networkStatus.isMetered) "Cellular/Metered" else "Unmetered / Wi-Fi"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        DiagnosticSectionCard(title = "2. MARKET REST API") {
            DiagRow(
                icon = Icons.Default.Speed,
                iconColor = CyanPrimary,
                label = "Active Provider",
                value = metrics.feedProvider,
                subtext = "Global / Non-blocking"
            )
            DiagRow(
                icon = Icons.Default.Speed,
                iconColor = CyanPrimary,
                label = "Base Endpoint",
                value = metrics.apiBaseUrl,
                subtext = "TLS 1.3 / Strict HTTPS"
            )
            DiagRow(
                icon = Icons.Default.CheckCircle,
                iconColor = if (metrics.apiStatus.contains("OK", ignoreCase = true)) BullishGreen else CyanPrimary,
                label = "API Status",
                value = metrics.apiStatus,
                valueColor = if (metrics.apiStatus.contains("OK", ignoreCase = true)) BullishGreen else TextPrimary
            )
            if (metrics.lastApiSuccessTime > 0) {
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(metrics.lastApiSuccessTime))
                DiagRow(
                    icon = Icons.Default.Sync,
                    iconColor = TextSecondary,
                    label = "Last Success",
                    value = "$timeStr (Latency: ${metrics.apiLatencyMs}ms)"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        DiagnosticSectionCard(title = "3. MARKET WEBSOCKET STREAM") {
            DiagRow(
                icon = Icons.Default.Lan,
                iconColor = CyanPrimary,
                label = "WebSocket URL",
                value = metrics.webSocketUrl,
                subtext = "Heartbeat ping every 15s"
            )
            val wsColor = when (metrics.webSocketState) {
                WebSocketConnectionState.CONNECTED -> BullishGreen
                WebSocketConnectionState.CONNECTING -> CyanPrimary
                WebSocketConnectionState.OFFLINE_PAUSED -> Color(0xFFFFB300)
                else -> BearishRed
            }
            DiagRow(
                icon = Icons.Default.Sync,
                iconColor = wsColor,
                label = "Connection State",
                value = metrics.webSocketState.name,
                valueColor = wsColor
            )
            DiagRow(
                icon = Icons.Default.CheckCircle,
                iconColor = CyanPrimary,
                label = "Live Messages Received",
                value = "%,d ticks".format(Locale.US, metrics.webSocketMessagesCount)
            )
            if (metrics.lastWebSocketMessageTime > 0) {
                val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(metrics.lastWebSocketMessageTime))
                DiagRow(
                    icon = Icons.Default.Speed,
                    iconColor = TextSecondary,
                    label = "Last Message Received",
                    value = timeStr
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        DiagnosticSectionCard(title = "4. AI BACKEND & SECRETS") {
            DiagRow(
                icon = Icons.Default.Key,
                iconColor = CyanPrimary,
                label = "Model Engine",
                value = "${metrics.aiModelName} (REST/Vision)"
            )
            DiagRow(
                icon = Icons.Default.CheckCircle,
                iconColor = if (metrics.aiApiStatus.contains("Success") || metrics.aiApiStatus.contains("200")) BullishGreen else CyanPrimary,
                label = "AI Engine Status",
                value = metrics.aiApiStatus,
                valueColor = if (metrics.aiApiStatus.contains("Success") || metrics.aiApiStatus.contains("200")) BullishGreen else TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Last Error Panel
        if (metrics.lastErrorMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BearishRedSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BearishRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = BearishRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LAST RECORDED ERROR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BearishRed
                                )
                            )
                        }
                        OutlinedButton(
                            onClick = onClearError,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = metrics.lastErrorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                    if (metrics.lastErrorTimestamp != null) {
                        val errTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(metrics.lastErrorTimestamp!!))
                        Text(
                            text = "Timestamp: $errTime",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Action Testing Buttons
        Text(
            text = "DIAGNOSTIC TEST CONTROLS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRetryConnection,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("retry_connection_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onTestApi,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("test_api_button")
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test API", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onTestWebSocket,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("test_websocket_button")
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test WS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Button(
                onClick = onTestAi,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("test_ai_button")
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test AI Key", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DiagnosticSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DiagRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    subtext: String? = null,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor
                )
            )
            if (subtext != null) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
