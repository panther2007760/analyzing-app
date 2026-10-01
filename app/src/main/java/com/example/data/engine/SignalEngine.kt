package com.example.data.engine

import com.example.model.CandleStick
import com.example.model.CryptoPair
import com.example.model.Timeframe
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

enum class SignalBias {
    LONG_BIAS,
    SHORT_BIAS,
    WAIT
}

enum class SignalStrength {
    STRONG_BULLISH,
    BULLISH,
    NEUTRAL,
    BEARISH,
    STRONG_BEARISH
}

data class TechnicalSignal(
    val pairSymbol: String,
    val timeframe: Timeframe,
    val timestamp: Long = System.currentTimeMillis(),
    val bias: SignalBias,
    val strength: SignalStrength,
    val confidencePercent: Int,
    val currentPrice: Double,
    val entryZone: String,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val riskRewardRatio: String,
    // Indicators
    val rsi: Double,
    val rsiDivergence: RSIDivergenceType,
    val ema9: Double,
    val ema21: Double,
    val ema50: Double,
    val ema200: Double,
    val macdHistogram: Double,
    val macdLine: Double,
    val macdSignal: Double,
    val bollingerBands: BollingerBandsResult,
    val atr: Double,
    val supportLevels: List<Double>,
    val resistanceLevels: List<Double>,
    val marketStructure: MarketStructureResult,
    val candlestickPattern: CandlestickPatternResult,
    // Multi-Timeframe
    val htfTrend: String,
    val ctfTrend: String,
    val ltfMomentum: String,
    // Candle timestamps
    val lastClosedCandleTime: Long,
    val currentLiveCandleTime: Long,
    // Explanations
    val reasons: List<String>,
    val invalidationCondition: String,
    val isMixedConflict: Boolean = false,
    val isStale: Boolean = false
)

object SignalEngine {

    fun generateSignal(
        pair: CryptoPair,
        timeframe: Timeframe,
        rawCandles: List<CandleStick>,
        currentPrice: Double,
        htfCandles: List<CandleStick>? = null
    ): TechnicalSignal {
        // Step 1: Validate and sanitize candles
        val validCandles = rawCandles
            .filter { it.isValid() }
            .distinctBy { it.openTime }
            .sortedBy { it.openTime }

        if (validCandles.size < 25 || currentPrice <= 0.0) {
            val emptyBB = BollingerBandsResult(currentPrice, currentPrice, currentPrice, 0.0, 50.0)
            val emptyStruct = MarketStructureResult("INSUFFICIENT_DATA", false, false, "Waiting for minimum 25 candles")
            val emptyPattern = CandlestickPatternResult(CandlestickPattern.NONE, false, "Insufficient candles")

            return TechnicalSignal(
                pairSymbol = pair.coinbaseProduct,
                timeframe = timeframe,
                bias = SignalBias.WAIT,
                strength = SignalStrength.NEUTRAL,
                confidencePercent = 0,
                currentPrice = currentPrice,
                entryZone = "Awaiting sufficient historical candles (min 25 candles)",
                stopLoss = 0.0,
                takeProfit1 = 0.0,
                takeProfit2 = 0.0,
                riskRewardRatio = "N/A",
                rsi = 50.0,
                rsiDivergence = RSIDivergenceType.NONE,
                ema9 = currentPrice,
                ema21 = currentPrice,
                ema50 = currentPrice,
                ema200 = currentPrice,
                macdHistogram = 0.0,
                macdLine = 0.0,
                macdSignal = 0.0,
                bollingerBands = emptyBB,
                atr = 0.0,
                supportLevels = emptyList(),
                resistanceLevels = emptyList(),
                marketStructure = emptyStruct,
                candlestickPattern = emptyPattern,
                htfTrend = "Unavailable",
                ctfTrend = "Unavailable",
                ltfMomentum = "Unavailable",
                lastClosedCandleTime = 0L,
                currentLiveCandleTime = System.currentTimeMillis(),
                reasons = listOf("Insufficient valid historical candles for mathematical indicators."),
                invalidationCondition = "Requires active real-time candle stream.",
                isMixedConflict = false
            )
        }

        // Distinguish live unclosed candle from closed history
        val lastClosedCandle = if (validCandles.size >= 2) validCandles[validCandles.size - 2] else validCandles.last()
        val currentLiveCandle = validCandles.last()

        val closes = validCandles.map { it.close }

        // Step 2: Calculate Technical Indicators
        val ema9List = IndicatorCalculator.calculateEMA(closes, 9)
        val ema21List = IndicatorCalculator.calculateEMA(closes, 21)
        val ema50List = IndicatorCalculator.calculateEMA(closes, 50)
        val ema200List = IndicatorCalculator.calculateEMA(closes, 200)

        val ema9 = ema9List.lastOrNull() ?: currentPrice
        val ema21 = ema21List.lastOrNull() ?: currentPrice
        val ema50 = ema50List.lastOrNull() ?: ema21
        val ema200 = ema200List.lastOrNull() ?: ema50

        val rsi = IndicatorCalculator.calculateRSI(closes, 14)
        val rsiDiv = IndicatorCalculator.detectRSIDivergence(validCandles)
        val macd = IndicatorCalculator.calculateMACD(closes)
        val bb = IndicatorCalculator.calculateBollingerBands(closes)
        val atr = IndicatorCalculator.calculateATR(validCandles)
        val sr = IndicatorCalculator.calculateSupportResistance(validCandles)
        val structure = IndicatorCalculator.analyzeMarketStructure(validCandles)
        val pattern = IndicatorCalculator.detectPattern(validCandles)

        // Step 3: Multi-Timeframe Synthesis
        val htfTrend = if (htfCandles != null && htfCandles.size >= 25) {
            val hCloses = htfCandles.map { it.close }
            val hEma9 = IndicatorCalculator.calculateEMA(hCloses, 9).lastOrNull() ?: 0.0
            val hEma21 = IndicatorCalculator.calculateEMA(hCloses, 21).lastOrNull() ?: 0.0
            if (hEma9 > hEma21) "BULLISH (HTF 1H EMA 9 > 21)" else "BEARISH (HTF 1H EMA 9 < 21)"
        } else {
            if (ema50 > ema200) "BULLISH (50 > 200)" else "BEARISH (50 < 200)"
        }

        val ctfTrend = if (ema9 > ema21) "BULLISH (EMA 9 > 21)" else "BEARISH (EMA 9 < 21)"
        val ltfMomentum = if (macd.histogram > 0) "POSITIVE MOMENTUM" else "NEGATIVE MOMENTUM"

        // Step 4: Transparent Scoring Algorithm
        var score = 0
        val reasons = ArrayList<String>()

        // 1. EMA Trend
        if (ema9 > ema21) {
            score += 20
            reasons.add("EMA 9 (${formatPrice(ema9)}) is above EMA 21 (${formatPrice(ema21)}) [Bullish Trend Alignment]")
        } else {
            score -= 20
            reasons.add("EMA 9 (${formatPrice(ema9)}) is below EMA 21 (${formatPrice(ema21)}) [Bearish Trend Alignment]")
        }

        if (currentPrice > ema50) {
            score += 10
            reasons.add("Price holds above 50-period EMA (${formatPrice(ema50)}) [Medium-term support]")
        } else {
            score -= 10
            reasons.add("Price trades below 50-period EMA (${formatPrice(ema50)}) [Medium-term resistance]")
        }

        // 2. RSI Evaluation
        if (rsi in 52.0..68.0) {
            score += 15
            reasons.add("RSI at ${String.format(Locale.US, "%.1f", rsi)} reflects healthy bullish momentum without exhaustion.")
        } else if (rsi in 32.0..48.0) {
            score -= 15
            reasons.add("RSI at ${String.format(Locale.US, "%.1f", rsi)} demonstrates steady seller momentum.")
        } else if (rsi > 70.0) {
            score -= 10
            reasons.add("RSI Overbought (${String.format(Locale.US, "%.1f", rsi)} > 70.0): Buyer exhaustion risk elevated.")
        } else if (rsi < 30.0) {
            score += 10
            reasons.add("RSI Oversold (${String.format(Locale.US, "%.1f", rsi)} < 30.0): Seller climax bounce potential.")
        }

        // RSI Divergence
        if (rsiDiv == RSIDivergenceType.BEARISH_DIVERGENCE) {
            score -= 25
            reasons.add("Warning: Bearish RSI Divergence detected (Price made higher high but RSI made lower high).")
        } else if (rsiDiv == RSIDivergenceType.BULLISH_DIVERGENCE) {
            score += 25
            reasons.add("Bullish RSI Divergence detected (Price made lower low but RSI made higher low).")
        }

        // 3. MACD
        if (macd.histogram > 0) {
            score += 15
            reasons.add("MACD Histogram is positive (+${String.format(Locale.US, "%.4f", macd.histogram)}) [Buyer dominance]")
        } else {
            score -= 15
            reasons.add("MACD Histogram is negative (${String.format(Locale.US, "%.4f", macd.histogram)}) [Seller dominance]")
        }

        if (macd.isStrengthening) {
            score += if (macd.histogram > 0) 5 else -5
        }

        // 4. Market Structure & S/R
        if (structure.trend.startsWith("BULLISH")) {
            score += 15
            reasons.add("Market Structure: ${structure.structureSummary}")
        } else if (structure.trend.startsWith("BEARISH")) {
            score -= 15
            reasons.add("Market Structure: ${structure.structureSummary}")
        }

        // 5. Candlestick Pattern on Closed Candle
        if (pattern.pattern != CandlestickPattern.NONE) {
            if (pattern.isBullish) {
                score += 10
                reasons.add("Candlestick: ${pattern.description}")
            } else {
                score -= 10
                reasons.add("Candlestick: ${pattern.description}")
            }
        }

        // 6. HTF Confirmation
        if (htfTrend.startsWith("BULLISH")) {
            score += 10
        } else if (htfTrend.startsWith("BEARISH")) {
            score -= 10
        }

        // Detect Conflict:
        val emaBullish = ema9 > ema21
        val emaBearish = ema9 < ema21
        val rsiConflict = (emaBullish && rsi > 70.0) || (emaBearish && rsi < 30.0) || (rsiDiv != RSIDivergenceType.NONE)
        val macdConflict = (emaBullish && macd.histogram < -0.0001) || (emaBearish && macd.histogram > 0.0001)

        val isConflict = rsiConflict || macdConflict || (abs(score) < 35)

        val bias: SignalBias
        val strength: SignalStrength
        val confidence: Int

        if (isConflict || abs(score) < 35) {
            bias = SignalBias.WAIT
            strength = SignalStrength.NEUTRAL
            confidence = (abs(score)).coerceIn(15, 48)
            reasons.add(0, "WAIT / MIXED: Indicators show opposing bias. Do not force an entry without multi-indicator consensus.")
        } else if (score >= 35) {
            bias = SignalBias.LONG_BIAS
            strength = if (score >= 65) SignalStrength.STRONG_BULLISH else SignalStrength.BULLISH
            confidence = score.coerceIn(55, 94)
        } else {
            bias = SignalBias.SHORT_BIAS
            strength = if (score <= -65) SignalStrength.STRONG_BEARISH else SignalStrength.BEARISH
            confidence = abs(score).coerceIn(55, 94)
        }

        // Dynamic targets and stop loss
        val riskUnit = max(atr * 1.5, currentPrice * 0.012)
        val stopLoss: Double
        val tp1: Double
        val tp2: Double
        val entryZone: String
        val invalidation: String

        if (bias == SignalBias.LONG_BIAS) {
            entryZone = "${formatPrice(currentPrice * 0.998)} - ${formatPrice(currentPrice * 1.002)}"
            stopLoss = currentPrice - riskUnit
            tp1 = currentPrice + (riskUnit * 1.5)
            tp2 = currentPrice + (riskUnit * 2.5)
            invalidation = "1-candle close below identified support (${formatPrice(stopLoss)}) invalidates the Long setup."
        } else if (bias == SignalBias.SHORT_BIAS) {
            entryZone = "${formatPrice(currentPrice * 0.998)} - ${formatPrice(currentPrice * 1.002)}"
            stopLoss = currentPrice + riskUnit
            tp1 = currentPrice - (riskUnit * 1.5)
            tp2 = currentPrice - (riskUnit * 2.5)
            invalidation = "1-candle close above identified resistance (${formatPrice(stopLoss)}) invalidates the Short setup."
        } else {
            entryZone = "WAIT - No entry zone active"
            stopLoss = 0.0
            tp1 = 0.0
            tp2 = 0.0
            invalidation = "Wait for EMA 9/21 realignment and RSI departure from extreme zones."
        }

        return TechnicalSignal(
            pairSymbol = pair.coinbaseProduct,
            timeframe = timeframe,
            timestamp = System.currentTimeMillis(),
            bias = bias,
            strength = strength,
            confidencePercent = confidence,
            currentPrice = currentPrice,
            entryZone = entryZone,
            stopLoss = stopLoss,
            takeProfit1 = tp1,
            takeProfit2 = tp2,
            riskRewardRatio = if (bias == SignalBias.WAIT) "N/A" else "1 : 1.5 (TP1) / 1 : 2.5 (TP2)",
            rsi = rsi,
            rsiDivergence = rsiDiv,
            ema9 = ema9,
            ema21 = ema21,
            ema50 = ema50,
            ema200 = ema200,
            macdHistogram = macd.histogram,
            macdLine = macd.macdLine,
            macdSignal = macd.signalLine,
            bollingerBands = bb,
            atr = atr,
            supportLevels = listOf(sr.s1, sr.s2, sr.swingLow),
            resistanceLevels = listOf(sr.r1, sr.r2, sr.swingHigh),
            marketStructure = structure,
            candlestickPattern = pattern,
            htfTrend = htfTrend,
            ctfTrend = ctfTrend,
            ltfMomentum = ltfMomentum,
            lastClosedCandleTime = lastClosedCandle.openTime,
            currentLiveCandleTime = currentLiveCandle.openTime,
            reasons = reasons,
            invalidationCondition = invalidation,
            isMixedConflict = isConflict
        )
    }

    private fun formatPrice(v: Double): String {
        return if (v >= 100.0) String.format(Locale.US, "$%.2f", v)
        else if (v >= 1.0) String.format(Locale.US, "$%.4f", v)
        else String.format(Locale.US, "$%.6f", v)
    }
}
