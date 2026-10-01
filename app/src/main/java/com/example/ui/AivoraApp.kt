package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MarketRepository
import com.example.model.AppStrings
import com.example.model.DataSourceMode
import com.example.ui.components.DemoModeBanner
import com.example.ui.components.OfflineWarningBanner
import com.example.ui.components.TopNetworkHeader
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.DebugScreen
import com.example.ui.screens.ForexScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LiveScreenCaptureScreen
import com.example.ui.screens.MarketScreen
import com.example.ui.screens.RiskCalculatorScreen
import com.example.ui.screens.ScreenshotAnalysisScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignalsScreen
import com.example.ui.theme.BorderDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class AppTab(val key: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    MARKETS("tab_markets", Icons.Default.CandlestickChart),
    SIGNALS("tab_signals", Icons.Default.Radar),
    SCREEN_AI("tab_screen", Icons.Default.DocumentScanner),
    AI_CHAT("tab_chat", Icons.AutoMirrored.Filled.Chat),
    SETTINGS("tab_tools", Icons.Default.Settings)
}

enum class SubScreen {
    NONE,
    ALERTS,
    HISTORY,
    FOREX,
    RISK_CALC,
    DEBUG
}

@Composable
fun AivoraApp(
    repository: MarketRepository,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(AppTab.MARKETS) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var screenAiMode by remember { mutableStateOf("UPLOAD") } // "UPLOAD" or "LIVE_PROJECTION"

    // Risk calc pre-fill values
    var riskCalcEntry by remember { mutableStateOf<Double?>(null) }
    var riskCalcSL by remember { mutableStateOf<Double?>(null) }

    val language by repository.appLanguage.collectAsStateWithLifecycle()
    val networkStatus by repository.networkMonitor.networkStatus.collectAsState()
    val dataSourceMode by repository.dataSourceMode.collectAsState()
    val selectedPair by repository.selectedPair.collectAsState()
    val ticker by repository.currentTicker.collectAsState()
    val candles by repository.candles.collectAsState()
    val isLoadingMarket by repository.isLoadingMarketData.collectAsState()
    val marketError by repository.marketErrorMessage.collectAsState()
    val webSocketState by repository.webSocketManager.connectionState.collectAsState()

    val liveAiAnalysis by repository.liveAiAnalysis.collectAsState()
    val isLiveAiLoading by repository.isLiveAiLoading.collectAsState()

    val screenshotAnalysis by repository.screenshotAiAnalysis.collectAsState()
    val isScreenshotLoading by repository.isScreenshotAiLoading.collectAsState()
    val debugMetrics by repository.debugMetrics.collectAsState()

    BackHandler(enabled = currentSubScreen != SubScreen.NONE || currentTab != AppTab.MARKETS) {
        if (currentSubScreen != SubScreen.NONE) {
            currentSubScreen = SubScreen.NONE
        } else {
            currentTab = AppTab.MARKETS
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopNetworkHeader(
                    networkStatus = networkStatus,
                    dataSourceMode = dataSourceMode,
                    onToggleDataSource = {
                        val newMode = if (dataSourceMode == DataSourceMode.LIVE) {
                            DataSourceMode.DEMO
                        } else {
                            DataSourceMode.LIVE
                        }
                        repository.toggleDataSourceMode(newMode)
                    }
                )

                // Warning banners
                OfflineWarningBanner(
                    networkStatus = networkStatus,
                    onRetry = { repository.retryConnection() }
                )

                if (dataSourceMode == DataSourceMode.DEMO) {
                    DemoModeBanner(
                        onSwitchToLive = { repository.toggleDataSourceMode(DataSourceMode.LIVE) }
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .border(width = 0.5.dp, color = BorderDark)
            ) {
                AppTab.entries.forEach { tab ->
                    val selected = currentTab == tab && currentSubScreen == SubScreen.NONE
                    val title = AppStrings.get(tab.key, language)

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            currentTab = tab
                            currentSubScreen = SubScreen.NONE
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Render sub-screen or main tab
            when (currentSubScreen) {
                SubScreen.ALERTS -> {
                    AlertsScreen(repository = repository)
                }
                SubScreen.HISTORY -> {
                    HistoryScreen(repository = repository)
                }
                SubScreen.FOREX -> {
                    ForexScreen(repository = repository)
                }
                SubScreen.RISK_CALC -> {
                    RiskCalculatorScreen(
                        repository = repository,
                        initialEntry = riskCalcEntry,
                        initialStopLoss = riskCalcSL
                    )
                }
                SubScreen.DEBUG -> {
                    DebugScreen(
                        networkStatus = networkStatus,
                        metrics = debugMetrics,
                        onRetryConnection = { repository.retryConnection() },
                        onTestApi = { repository.testApiConnection() },
                        onTestWebSocket = { repository.testWebSocket() },
                        onTestAi = { repository.testAiConnection() },
                        onClearError = { repository.clearLastError() }
                    )
                }
                SubScreen.NONE -> {
                    when (currentTab) {
                        AppTab.MARKETS -> {
                            MarketScreen(
                                repository = repository,
                                selectedPair = selectedPair,
                                ticker = ticker,
                                candles = candles,
                                webSocketState = webSocketState,
                                isLive = dataSourceMode == DataSourceMode.LIVE,
                                isLoading = isLoadingMarket,
                                errorMessage = marketError,
                                aiAnalysis = liveAiAnalysis,
                                isAiLoading = isLiveAiLoading,
                                onSelectPair = { pair -> repository.selectPair(pair) },
                                onRefresh = { repository.loadMarketData() },
                                onRunAiAnalysis = { repository.runLiveTechnicalAnalysis() }
                            )
                        }

                        AppTab.SIGNALS -> {
                            SignalsScreen(
                                repository = repository,
                                onNavigateToRiskCalc = { entry: Double, sl: Double ->
                                    riskCalcEntry = entry
                                    riskCalcSL = sl
                                    currentSubScreen = SubScreen.RISK_CALC
                                }
                            )
                        }

                        AppTab.SCREEN_AI -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Mode Toggle bar (Photo Upload vs MediaProjection Live Screen)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { screenAiMode = "UPLOAD" },
                                        color = if (screenAiMode == "UPLOAD") CyanPrimary.copy(alpha = 0.2f) else SurfaceCard,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, tint = if (screenAiMode == "UPLOAD") CyanPrimary else TextSecondary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "PHOTO / PRESET",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (screenAiMode == "UPLOAD") CyanPrimary else TextSecondary
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { screenAiMode = "LIVE_PROJECTION" },
                                        color = if (screenAiMode == "LIVE_PROJECTION") CyanPrimary.copy(alpha = 0.2f) else SurfaceCard,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Cast, contentDescription = null, tint = if (screenAiMode == "LIVE_PROJECTION") CyanPrimary else TextSecondary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "SCREEN CAPTURE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (screenAiMode == "LIVE_PROJECTION") CyanPrimary else TextSecondary
                                            )
                                        }
                                    }
                                }

                                if (screenAiMode == "UPLOAD") {
                                    ScreenshotAnalysisScreen(
                                        analysis = screenshotAnalysis,
                                        isLoading = isScreenshotLoading,
                                        onAnalyzeBitmap = { bmp, label ->
                                            repository.runScreenshotAnalysis(bmp, label)
                                        }
                                    )
                                } else {
                                    LiveScreenCaptureScreen(repository = repository)
                                }
                            }
                        }

                        AppTab.AI_CHAT -> {
                            AiChatScreen(repository = repository)
                        }

                        AppTab.SETTINGS -> {
                            SettingsScreen(
                                repository = repository,
                                onNavigateToAlerts = { currentSubScreen = SubScreen.ALERTS },
                                onNavigateToHistory = { currentSubScreen = SubScreen.HISTORY },
                                onNavigateToForex = { currentSubScreen = SubScreen.FOREX },
                                onNavigateToRiskCalc = { currentSubScreen = SubScreen.RISK_CALC },
                                onNavigateToDebug = { currentSubScreen = SubScreen.DEBUG }
                            )
                        }
                    }
                }
            }
        }
    }
}
