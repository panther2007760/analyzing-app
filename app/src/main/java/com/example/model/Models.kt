package com.example.model

enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER,
    NONE
}

data class NetworkStatus(
    val isConnected: Boolean = false,
    val networkType: NetworkType = NetworkType.NONE,
    val isMetered: Boolean = false,
    val hasInternetCapability: Boolean = false,
    val lastCheckedTimestamp: Long = System.currentTimeMillis(),
    val interfaceDetails: String = "Detecting..."
)

enum class DataSourceMode {
    LIVE,
    DEMO
}

enum class MarketType(val displayName: String) {
    CRYPTO("Crypto"),
    FOREX("Forex"),
    STOCKS("Stocks"),
    INDICES("Indices"),
    COMMODITIES("Commodities")
}

data class MarketInstrument(
    val id: String,
    val symbol: String,
    val baseAsset: String,
    val quoteAsset: String,
    val displayName: String,
    val iconSymbol: String,
    val marketType: MarketType,
    val exchangeProvider: String = "Coinbase",
    val isFuturesSupported: Boolean = true
) {
    fun toCryptoPair(): CryptoPair {
        return CryptoPair(
            id = id,
            symbol = symbol,
            baseAsset = baseAsset,
            quoteAsset = quoteAsset,
            displayName = displayName,
            iconSymbol = iconSymbol,
            coinbaseProduct = if (marketType == MarketType.CRYPTO) symbol else "$baseAsset-$quoteAsset",
            binanceSymbol = "${baseAsset}USDT"
        )
    }

    companion object {
        val ALL_INSTRUMENTS = listOf(
            // Crypto
            MarketInstrument("BTC", "BTC-USD", "BTC", "USD", "Bitcoin", "₿", MarketType.CRYPTO, "Coinbase"),
            MarketInstrument("ETH", "ETH-USD", "ETH", "USD", "Ethereum", "Ξ", MarketType.CRYPTO, "Coinbase"),
            MarketInstrument("SOL", "SOL-USD", "SOL", "USD", "Solana", "◎", MarketType.CRYPTO, "Coinbase"),
            MarketInstrument("BNB", "BNB-USD", "BNB", "USD", "BNB Chain", "Ƀ", MarketType.CRYPTO, "Binance"),
            MarketInstrument("XRP", "XRP-USD", "XRP", "USD", "XRP", "✕", MarketType.CRYPTO, "Coinbase"),
            MarketInstrument("DOGE", "DOGE-USD", "DOGE", "USD", "Dogecoin", "Ð", MarketType.CRYPTO, "Coinbase"),
            MarketInstrument("ADA", "ADA-USD", "ADA", "USD", "Cardano", "₳", MarketType.CRYPTO, "Coinbase"),
            MarketInstrument("AVAX", "AVAX-USD", "AVAX", "USD", "Avalanche", "▲", MarketType.CRYPTO, "Coinbase"),
            MarketInstrument("LINK", "LINK-USD", "LINK", "USD", "Chainlink", "⬡", MarketType.CRYPTO, "Coinbase"),
            MarketInstrument("DOT", "DOT-USD", "DOT", "USD", "Polkadot", "●", MarketType.CRYPTO, "Coinbase"),

            // Forex (ECB Reference)
            MarketInstrument("EURUSD", "EUR/USD", "EUR", "USD", "Euro / US Dollar", "€", MarketType.FOREX, "ECB"),
            MarketInstrument("GBPUSD", "GBP/USD", "GBP", "USD", "British Pound / US Dollar", "£", MarketType.FOREX, "ECB"),
            MarketInstrument("USDJPY", "USD/JPY", "USD", "JPY", "US Dollar / Japanese Yen", "¥", MarketType.FOREX, "ECB"),
            MarketInstrument("USDCHF", "USD/CHF", "USD", "CHF", "US Dollar / Swiss Franc", "Fr", MarketType.FOREX, "ECB"),
            MarketInstrument("AUDUSD", "AUD/USD", "AUD", "USD", "Australian Dollar / US Dollar", "A$", MarketType.FOREX, "ECB"),
            MarketInstrument("USDCAD", "USD/CAD", "USD", "CAD", "US Dollar / Canadian Dollar", "C$", MarketType.FOREX, "ECB"),
            MarketInstrument("NZDUSD", "NZD/USD", "NZD", "USD", "New Zealand Dollar / US Dollar", "NZ$", MarketType.FOREX, "ECB"),

            // Commodities
            MarketInstrument("XAU", "XAU/USD", "XAU", "USD", "Gold Spot", "🪙", MarketType.COMMODITIES, "MetalQuote"),
            MarketInstrument("XAG", "XAG/USD", "XAG", "USD", "Silver Spot", "🥈", MarketType.COMMODITIES, "MetalQuote"),
            MarketInstrument("BRENT", "BRENT", "BRENT", "USD", "Brent Crude Oil", "🛢️", MarketType.COMMODITIES, "EnergyQuote"),
            MarketInstrument("WTI", "WTI", "WTI", "USD", "WTI Crude Oil", "🛢️", MarketType.COMMODITIES, "EnergyQuote"),

            // Stocks
            MarketInstrument("AAPL", "AAPL", "AAPL", "USD", "Apple Inc.", "", MarketType.STOCKS, "GlobalEquities"),
            MarketInstrument("MSFT", "MSFT", "MSFT", "USD", "Microsoft Corp.", "⊞", MarketType.STOCKS, "GlobalEquities"),
            MarketInstrument("NVDA", "NVDA", "NVDA", "USD", "NVIDIA Corp.", "👁", MarketType.STOCKS, "GlobalEquities"),
            MarketInstrument("TSLA", "TSLA", "TSLA", "USD", "Tesla Inc.", "⚡", MarketType.STOCKS, "GlobalEquities"),

            // Indices
            MarketInstrument("SPX", "SPX", "SPX", "USD", "S&P 500 Index", "📊", MarketType.INDICES, "IndexRates"),
            MarketInstrument("NDX", "NDX", "NDX", "USD", "Nasdaq 100 Index", "📈", MarketType.INDICES, "IndexRates")
        )
    }
}

data class CryptoPair(
    val id: String,
    val symbol: String, // "BTC-USD" or "BTCUSDT"
    val baseAsset: String, // "BTC"
    val quoteAsset: String, // "USD"
    val displayName: String, // "Bitcoin"
    val iconSymbol: String = "₿",
    val binanceSymbol: String = "${baseAsset}USDT",
    val coinbaseProduct: String = "${baseAsset}-USD"
) {
    companion object {
        val DEFAULT_PAIRS = listOf(
            CryptoPair("BTC", "BTC-USD", "BTC", "USD", "Bitcoin", "₿", "BTCUSDT", "BTC-USD"),
            CryptoPair("ETH", "ETH-USD", "ETH", "USD", "Ethereum", "Ξ", "ETHUSDT", "ETH-USD"),
            CryptoPair("SOL", "SOL-USD", "SOL", "USD", "Solana", "◎", "SOLUSDT", "SOL-USD"),
            CryptoPair("BNB", "BNB-USD", "BNB", "USD", "BNB Chain", "Ƀ", "BNBUSDT", "BNB-USD"),
            CryptoPair("XRP", "XRP-USD", "XRP", "USD", "XRP", "✕", "XRPUSDT", "XRP-USD"),
            CryptoPair("DOGE", "DOGE-USD", "DOGE", "USD", "Dogecoin", "Ð", "DOGEUSDT", "DOGE-USD"),
            CryptoPair("ADA", "ADA-USD", "ADA", "USD", "Cardano", "₳", "ADAUSDT", "ADA-USD"),
            CryptoPair("AVAX", "AVAX-USD", "AVAX", "USD", "Avalanche", "▲", "AVAXUSDT", "AVAX-USD"),
            CryptoPair("LINK", "LINK-USD", "LINK", "USD", "Chainlink", "⬡", "LINKUSDT", "LINK-USD"),
            CryptoPair("DOT", "DOT-USD", "DOT", "USD", "Polkadot", "●", "DOTUSDT", "DOT-USD")
        )
    }
}

data class Ticker(
    val symbol: String,
    val lastPrice: Double,
    val priceChange24h: Double,
    val percentChange24h: Double,
    val highPrice24h: Double,
    val lowPrice24h: Double,
    val volume24h: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isLive: Boolean = true
)

enum class Timeframe(
    val label: String,
    val coinbaseGranularitySeconds: Int,
    val binanceInterval: String,
    val durationMs: Long
) {
    M1("1m", 60, "1m", 60_000L),
    M3("3m", 180, "3m", 180_000L),
    M5("5m", 300, "5m", 300_000L),
    M15("15m", 900, "15m", 900_000L),
    M30("30m", 1800, "30m", 1_800_000L),
    H1("1H", 3600, "1h", 3_600_000L),
    H4("4H", 21600, "4h", 14_400_000L),
    D1("1D", 86400, "1d", 86_400_000L),
    W1("1W", 604800, "1w", 604_800_000L)
}

enum class MarketDataStatus {
    LIVE,
    CONNECTING,
    STALE,
    OFFLINE,
    ERROR
}

data class CandleStick(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long = openTime + 3600000L - 1L
) {
    fun isValid(): Boolean {
        return openTime > 0 &&
                open > 0.0 &&
                close > 0.0 &&
                high >= open &&
                high >= close &&
                low <= open &&
                low <= close &&
                low > 0.0 &&
                volume >= 0.0
    }
}

enum class ChartType {
    CANDLESTICK,
    LINE,
    AREA
}

enum class DrawingTool {
    NONE,
    HORIZONTAL_LINE,
    TREND_LINE,
    PRICE_LEVEL,
    ZONE
}

data class ChartDrawing(
    val tool: DrawingTool,
    val price1: Double,
    val price2: Double = price1,
    val time1: Long = 0L,
    val time2: Long = 0L,
    val label: String = ""
)

enum class PatternMatchLevel(val label: String) {
    STRONG_MATCH("Strong Match"),
    PARTIAL_MATCH("Partial Match"),
    WEAK_MATCH("Weak Match"),
    INSUFFICIENT_DATA("Insufficient Data"),
    NO_MATCH("No Match")
}

data class PatternMatchResult(
    val level: PatternMatchLevel,
    val matchedPatternName: String?,
    val matchScorePercent: Int,
    val historicalSuccessRate: Double,
    val explanation: String
)

enum class MarketTrend {
    BULLISH,
    BEARISH,
    NEUTRAL,
    CONSOLIDATION
}

data class AIAnalysis(
    val title: String,
    val analysisType: AnalysisType,
    val trend: MarketTrend,
    val confidencePercent: Int,
    val keyPatterns: List<String>,
    val supportLevels: List<Double>,
    val resistanceLevels: List<Double>,
    val indicatorsSummary: String,
    val technicalVerdict: String,
    val riskAssessment: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false,
    val disclaimer: String = "Educational technical analysis only. No trading execution or financial advice."
)

enum class AnalysisType {
    LIVE_MARKET_DATA,
    LOCAL_SCREENSHOT
}

enum class WebSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    OFFLINE_PAUSED,
    ERROR
}

data class DebugMetrics(
    val isInternetConnected: Boolean = false,
    val networkType: String = "None",
    val feedProvider: String = "Coinbase Exchange (Global)",
    val apiBaseUrl: String = "https://api.exchange.coinbase.com",
    val apiStatus: String = "Not Tested",
    val apiLatencyMs: Long = 0,
    val lastApiSuccessTime: Long = 0,
    val webSocketUrl: String = "wss://ws-feed.exchange.coinbase.com",
    val webSocketState: WebSocketConnectionState = WebSocketConnectionState.DISCONNECTED,
    val webSocketMessagesCount: Long = 0,
    val lastWebSocketMessageTime: Long = 0,
    val aiApiStatus: String = "Ready",
    val aiModelName: String = "gemini-3.5-flash",
    val lastErrorMessage: String? = null,
    val lastErrorTimestamp: Long? = null
)
