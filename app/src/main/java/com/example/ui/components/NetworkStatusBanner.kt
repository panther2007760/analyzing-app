package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DataSourceMode
import com.example.model.NetworkStatus
import com.example.model.NetworkType
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedSoft
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenSoft
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DemoAmber
import com.example.ui.theme.DemoAmberSoft
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TopNetworkHeader(
    networkStatus: NetworkStatus,
    dataSourceMode: DataSourceMode,
    onToggleDataSource: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceCard,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: App Name + Pulse
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (networkStatus.isConnected) BullishGreen else BearishRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AIVORA CHART AI",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                )
            }

            // Right: Connectivity & Data Source Pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Online/Offline Pill
                StatusPill(
                    label = if (networkStatus.isConnected) "ONLINE" else "OFFLINE",
                    icon = when {
                        !networkStatus.isConnected -> Icons.Default.WifiOff
                        networkStatus.networkType == NetworkType.WIFI -> Icons.Default.Wifi
                        networkStatus.networkType == NetworkType.CELLULAR -> Icons.Default.CellTower
                        else -> Icons.Default.CheckCircle
                    },
                    backgroundColor = if (networkStatus.isConnected) BullishGreenSoft else BearishRedSoft,
                    textColor = if (networkStatus.isConnected) BullishGreen else BearishRed
                )

                // Live / Demo Pill (clickable)
                StatusPill(
                    label = if (dataSourceMode == DataSourceMode.LIVE) "LIVE DATA" else "DEMO DATA",
                    icon = if (dataSourceMode == DataSourceMode.LIVE) Icons.Default.Refresh else Icons.Default.Warning,
                    backgroundColor = if (dataSourceMode == DataSourceMode.LIVE) Color(0xFF0C2A38) else DemoAmberSoft,
                    textColor = if (dataSourceMode == DataSourceMode.LIVE) CyanPrimary else DemoAmber,
                    modifier = Modifier.clickable { onToggleDataSource() }
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(width = 0.5.dp, color = textColor.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = textColor
                )
            )
        }
    }
}

@Composable
fun OfflineWarningBanner(
    networkStatus: NetworkStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !networkStatus.isConnected,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BearishRedSoft)
                .border(1.dp, BearishRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(14.dp)
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
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "No Internet",
                        tint = BearishRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "No Internet Connection",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Market WebSocket paused. Check Wi-Fi or mobile data.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary
                            )
                        )
                    }
                }

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("retry_network_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Retry",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun DemoModeBanner(
    onSwitchToLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DemoAmberSoft)
            .border(1.dp, DemoAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Demo Mode",
                    tint = DemoAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "DEMO MODE ACTIVE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DemoAmber
                        )
                    )
                    Text(
                        text = "Displaying simulated offline chart data. Prices are not live.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSwitchToLive,
                colors = ButtonDefaults.buttonColors(containerColor = DemoAmber),
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "Go Live",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
