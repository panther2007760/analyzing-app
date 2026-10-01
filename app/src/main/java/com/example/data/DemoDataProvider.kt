package com.example.data

import com.example.model.AIAnalysis
import com.example.model.AnalysisType
import com.example.model.CandleStick
import com.example.model.CryptoPair
import com.example.model.MarketTrend
import com.example.model.Ticker

object DemoDataProvider {

    fun getDemoTicker(pair: CryptoPair): Ticker {
        val (price, change, high, low, vol) = when (pair.symbol) {
            "BTCUSDT" -> Quintuple(67450.0, 2.34, 68200.0, 65890.0, 28410.5)
            "ETHUSDT" -> Quintuple(3520.0, -1.12, 3610.0, 3480.0, 185400.0)
            "SOLUSDT" -> Quintuple(178.50, 4.85, 184.20, 169.10, 492000.0)
            "BNBUSDT" -> Quintuple(595.0, 0.45, 604.0, 588.0, 89300.0)
            "XRPUSDT" -> Quintuple(0.582, -0.75, 0.601, 0.575, 9400000.0)
            "DOGEUSDT" -> Quintuple(0.125, 3.20, 0.131, 0.120, 24500000.0)
            else -> Quintuple(100.0, 0.0, 105.0, 95.0, 1000.0)
        }

        return Ticker(
            symbol = pair.symbol,
            lastPrice = price,
            priceChange24h = (price * (change / 100.0)),
            percentChange24h = change,
            highPrice24h = high,
            lowPrice24h = low,
            volume24h = vol,
            timestamp = System.currentTimeMillis(),
            isLive = false // STRICTLY flagged as not live
        )
    }

    fun getDemoCandles(basePrice: Double): List<CandleStick> {
        val list = mutableListOf<CandleStick>()
        var current = basePrice * 0.95
        val now = System.currentTimeMillis()
        val interval = 3600 * 1000L // 1 hour

        val deltas = listOf(
            0.004, -0.002, 0.006, 0.008, -0.003, 0.001, -0.005, 0.009,
            0.012, -0.004, 0.007, -0.002, 0.015, -0.006, 0.008, 0.003,
            -0.007, 0.005, 0.011, -0.003, 0.004, 0.009, -0.002, 0.006
        )

        for (i in 0 until 30) {
            val d = deltas[i % deltas.size]
            val open = current
            val close = open * (1.0 + d)
            val high = maxOf(open, close) * (1.0 + Math.abs(d) * 0.5)
            val low = minOf(open, close) * (1.0 - Math.abs(d) * 0.5)
            val vol = (basePrice * 2.5) * (0.8 + (i % 5) * 0.1)

            val openTime = now - (30 - i) * interval
            val closeTime = openTime + interval - 1

            list.add(
                CandleStick(
                    openTime = openTime,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = vol,
                    closeTime = closeTime
                )
            )
            current = close
        }

        return list
    }

    fun getDemoAnalysis(pair: CryptoPair): AIAnalysis {
        return AIAnalysis(
            title = "[DEMO MODE] ${pair.displayName} Offline Analysis",
            analysisType = AnalysisType.LIVE_MARKET_DATA,
            trend = MarketTrend.BULLISH,
            confidencePercent = 82,
            keyPatterns = listOf("Ascending Triangle Consolidation", "Bullish Moving Average Cross"),
            supportLevels = listOf(64500.0, 62800.0),
            resistanceLevels = listOf(68500.0, 71200.0),
            indicatorsSummary = "RSI resting around 58.0 (Neutral-Bullish). 20-EMA holding above 50-EMA on 4H horizon.",
            technicalVerdict = "Consolidating beneath overhead psychological resistance. A decisive high-volume hourly candle close is required to confirm upward breakout continuation.",
            riskAssessment = "Strict invalidation below major support shelf. Educational preview only; real live connectivity is required for live signals.",
            timestamp = System.currentTimeMillis(),
            isDemo = true
        )
    }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )
}
