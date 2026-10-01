package com.example.data.engine

import com.example.model.CandleStick
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class MACDResult(
    val macdLine: Double,
    val signalLine: Double,
    val histogram: Double,
    val isStrengthening: Boolean
)

data class BollingerBandsResult(
    val upper: Double,
    val middle: Double,
    val lower: Double,
    val bandwidth: Double,
    val percentB: Double
)

data class SupportResistanceLevels(
    val pivot: Double,
    val r1: Double,
    val r2: Double,
    val s1: Double,
    val s2: Double,
    val swingHigh: Double,
    val swingLow: Double
)

enum class CandlestickPattern {
    BULLISH_ENGULFING,
    BEARISH_ENGULFING,
    HAMMER,
    SHOOTING_STAR,
    DOJI,
    MORNING_STAR,
    EVENING_STAR,
    PIN_BAR,
    INSIDE_BAR,
    NONE
}

data class CandlestickPatternResult(
    val pattern: CandlestickPattern,
    val isBullish: Boolean,
    val description: String
)

enum class RSIDivergenceType {
    BULLISH_DIVERGENCE,
    BEARISH_DIVERGENCE,
    NONE
}

data class MarketStructureResult(
    val trend: String, // "BULLISH_HH_HL", "BEARISH_LH_LL", "CONSOLIDATION_RANGING"
    val isBOS: Boolean,
    val isCHoCH: Boolean,
    val structureSummary: String
)

object IndicatorCalculator {

    /**
     * Exponential Moving Average (EMA)
     */
    fun calculateEMA(values: List<Double>, period: Int): List<Double> {
        if (values.size < period || period <= 0) return emptyList()

        val k = 2.0 / (period + 1.0)
        val emaList = ArrayList<Double>(values.size)

        var sma = 0.0
        for (i in 0 until period) {
            sma += values[i]
        }
        sma /= period
        emaList.add(sma)

        for (i in period until values.size) {
            val prevEma = emaList.last()
            val currentEma = (values[i] * k) + (prevEma * (1.0 - k))
            emaList.add(currentEma)
        }

        return emaList
    }

    /**
     * Relative Strength Index (RSI 14) with Wilder's Smoothing
     */
    fun calculateRSI(closes: List<Double>, period: Int = 14): Double {
        if (closes.size <= period) return 50.0

        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val change = closes[i] - closes[i - 1]
            if (change > 0) gains += change else losses += abs(change)
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in (period + 1) until closes.size) {
            val change = closes[i] - closes[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period
        }

        if (avgLoss == 0.0 && avgGain == 0.0) return 50.0
        if (avgLoss == 0.0) return 100.0
        if (avgGain == 0.0) return 0.0

        val rs = avgGain / avgLoss
        val rsi = 100.0 - (100.0 / (1.0 + rs))
        return rsi.coerceIn(0.0, 100.0)
    }

    /**
     * Moving Average Convergence Divergence (MACD 12, 26, 9)
     */
    fun calculateMACD(
        closes: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MACDResult {
        if (closes.size < slowPeriod + signalPeriod) {
            return MACDResult(0.0, 0.0, 0.0, false)
        }

        val fastEma = calculateEMA(closes, fastPeriod)
        val slowEma = calculateEMA(closes, slowPeriod)

        val offset = slowPeriod - fastPeriod
        val macdSeries = ArrayList<Double>()

        for (i in slowEma.indices) {
            val fastIndex = i + offset
            if (fastIndex < fastEma.size) {
                macdSeries.add(fastEma[fastIndex] - slowEma[i])
            }
        }

        if (macdSeries.size < signalPeriod) {
            val lastMacd = macdSeries.lastOrNull() ?: 0.0
            return MACDResult(lastMacd, 0.0, lastMacd, false)
        }

        val signalEma = calculateEMA(macdSeries, signalPeriod)
        val currentMacd = macdSeries.last()
        val currentSignal = signalEma.lastOrNull() ?: currentMacd
        val histogram = currentMacd - currentSignal

        // Check momentum strengthening
        val prevHist = if (signalEma.size >= 2 && macdSeries.size >= 2) {
            macdSeries[macdSeries.size - 2] - signalEma[signalEma.size - 2]
        } else histogram

        val isStrengthening = abs(histogram) > abs(prevHist)

        return MACDResult(currentMacd, currentSignal, histogram, isStrengthening)
    }

    /**
     * Bollinger Bands (20 period, 2.0 stdDev)
     */
    fun calculateBollingerBands(
        closes: List<Double>,
        period: Int = 20,
        numStdDev: Double = 2.0
    ): BollingerBandsResult {
        if (closes.size < period) {
            val price = closes.lastOrNull() ?: 0.0
            return BollingerBandsResult(price, price, price, 0.0, 50.0)
        }

        val window = closes.takeLast(period)
        val sma = window.average()
        val variance = window.map { (it - sma) * (it - sma) }.average()
        val stdDev = sqrt(variance)

        val upper = sma + (numStdDev * stdDev)
        val lower = sma - (numStdDev * stdDev)
        val bandwidth = if (sma > 0.0) ((upper - lower) / sma) * 100.0 else 0.0
        val last = closes.last()
        val percentB = if (upper != lower) ((last - lower) / (upper - lower)) * 100.0 else 50.0

        return BollingerBandsResult(upper, sma, lower, bandwidth, percentB)
    }

    /**
     * Average True Range (ATR 14)
     */
    fun calculateATR(candles: List<CandleStick>, period: Int = 14): Double {
        if (candles.size < 2) return 0.0

        val trList = ArrayList<Double>()
        for (i in 1 until candles.size) {
            val curr = candles[i]
            val prev = candles[i - 1]
            val tr = max(curr.high - curr.low, max(abs(curr.high - prev.close), abs(curr.low - prev.close)))
            trList.add(tr)
        }

        if (trList.size < period) return trList.average()

        var atr = trList.take(period).average()
        for (i in period until trList.size) {
            atr = ((atr * (period - 1)) + trList[i]) / period
        }
        return atr
    }

    /**
     * Support & Resistance Zones from Classical Pivots and Swing Points
     */
    fun calculateSupportResistance(candles: List<CandleStick>): SupportResistanceLevels {
        if (candles.isEmpty()) {
            return SupportResistanceLevels(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val high = candles.maxOf { it.high }
        val low = candles.minOf { it.low }
        val close = candles.last().close

        val pivot = (high + low + close) / 3.0
        val r1 = (2.0 * pivot) - low
        val s1 = (2.0 * pivot) - high
        val r2 = pivot + (high - low)
        val s2 = pivot - (high - low)

        // Find recent swing high and low (local extrema in past 20 candles)
        val recentWindow = candles.takeLast(25)
        val swingHigh = recentWindow.maxOfOrNull { it.high } ?: high
        val swingLow = recentWindow.minOfOrNull { it.low } ?: low

        return SupportResistanceLevels(pivot, r1, r2, s1, s2, swingHigh, swingLow)
    }

    /**
     * Market Structure Analysis: Higher Highs / Higher Lows vs Lower Highs / Lower Lows
     */
    fun analyzeMarketStructure(candles: List<CandleStick>): MarketStructureResult {
        if (candles.size < 15) {
            return MarketStructureResult(
                trend = "INSUFFICIENT_DATA",
                isBOS = false,
                isCHoCH = false,
                structureSummary = "Awaiting candle depth for market structure"
            )
        }

        // Find local swing highs and swing lows (fractal points)
        val swingHighs = ArrayList<Pair<Int, Double>>()
        val swingLows = ArrayList<Pair<Int, Double>>()

        for (i in 2 until candles.size - 2) {
            val c = candles[i]
            if (c.high > candles[i - 1].high && c.high > candles[i - 2].high &&
                c.high >= candles[i + 1].high && c.high >= candles[i + 2].high
            ) {
                swingHighs.add(i to c.high)
            }
            if (c.low < candles[i - 1].low && c.low < candles[i - 2].low &&
                c.low <= candles[i + 1].low && c.low <= candles[i + 2].low
            ) {
                swingLows.add(i to c.low)
            }
        }

        var isBullish = false
        var isBearish = false
        var isBOS = false
        var isCHoCH = false

        if (swingHighs.size >= 2 && swingLows.size >= 2) {
            val lastSH = swingHighs.last().second
            val prevSH = swingHighs[swingHighs.size - 2].second
            val lastSL = swingLows.last().second
            val prevSL = swingLows[swingLows.size - 2].second

            val higherHigh = lastSH > prevSH
            val higherLow = lastSL > prevSL
            val lowerHigh = lastSH < prevSH
            val lowerLow = lastSL < prevSL

            val currentPrice = candles.last().close

            if (higherHigh && higherLow) {
                isBullish = true
                if (currentPrice > lastSH) isBOS = true
            } else if (lowerHigh && lowerLow) {
                isBearish = true
                if (currentPrice < lastSL) isBOS = true
            } else if (higherHigh && lowerLow) {
                // Expanding / Volatile
            } else if (lowerHigh && higherLow) {
                // Contracting / Symmetrical Triangle
            }

            // CHoCH detection:
            if (isBearish && currentPrice > lastSH) {
                isCHoCH = true
            } else if (isBullish && currentPrice < lastSL) {
                isCHoCH = true
            }
        }

        val trend = when {
            isCHoCH -> "CHANGE_OF_CHARACTER (Reversal in progress)"
            isBullish -> "BULLISH_STRUCTURE (Higher Highs & Higher Lows)"
            isBearish -> "BEARISH_STRUCTURE (Lower Highs & Lower Lows)"
            else -> "CONSOLIDATION_RANGING (Sideways bound)"
        }

        val summary = buildString {
            append(trend)
            if (isBOS) append(" • Break of Structure (BOS) Confirmed")
            if (isCHoCH) append(" • Change of Character (CHoCH) Detected")
        }

        return MarketStructureResult(trend, isBOS, isCHoCH, summary)
    }

    /**
     * Candlestick Pattern Detection on the LAST CLOSED CANDLE
     */
    fun detectPattern(candles: List<CandleStick>): CandlestickPatternResult {
        // Need at least 3 candles to evaluate closed pattern
        if (candles.size < 3) {
            return CandlestickPatternResult(CandlestickPattern.NONE, false, "Insufficient candles")
        }

        // candle[candles.size - 1] may be the live unclosed candle; evaluate candle[candles.size - 2] as last closed
        val lastClosed = candles[candles.size - 2]
        val prevClosed = candles[candles.size - 3]
        val priorClosed = if (candles.size >= 4) candles[candles.size - 4] else null

        val body = abs(lastClosed.close - lastClosed.open)
        val range = lastClosed.high - lastClosed.low

        if (range <= 0.0) {
            return CandlestickPatternResult(CandlestickPattern.NONE, false, "No range")
        }

        // Doji: body <= 10% of range
        if (body / range <= 0.10) {
            return CandlestickPatternResult(
                CandlestickPattern.DOJI,
                false,
                "Doji: Market indecision between buyers and sellers on closed candle."
            )
        }

        // Bullish Engulfing: prev is red, last is green and covers prev body
        if (prevClosed.close < prevClosed.open && lastClosed.close > lastClosed.open) {
            if (lastClosed.open <= prevClosed.close && lastClosed.close >= prevClosed.open) {
                return CandlestickPatternResult(
                    CandlestickPattern.BULLISH_ENGULFING,
                    true,
                    "Bullish Engulfing: Strong buying pressure completely overtook previous sellers."
                )
            }
        }

        // Bearish Engulfing: prev is green, last is red and covers prev body
        if (prevClosed.close > prevClosed.open && lastClosed.close < lastClosed.open) {
            if (lastClosed.open >= prevClosed.close && lastClosed.close <= prevClosed.open) {
                return CandlestickPatternResult(
                    CandlestickPattern.BEARISH_ENGULFING,
                    false,
                    "Bearish Engulfing: Heavy selling pressure completely consumed previous buyers."
                )
            }
        }

        // Hammer / Pin Bar
        val lowerShadow = min(lastClosed.open, lastClosed.close) - lastClosed.low
        val upperShadow = lastClosed.high - max(lastClosed.open, lastClosed.close)

        // Hammer: lower wick >= 2x body, tiny upper wick
        if (lowerShadow >= (2.0 * body) && upperShadow <= (body * 0.4)) {
            return CandlestickPatternResult(
                CandlestickPattern.HAMMER,
                true,
                "Hammer (Bullish Rejection): Buyers aggressively defended low prices."
            )
        }

        // Shooting Star: upper wick >= 2x body, tiny lower wick
        if (upperShadow >= (2.0 * body) && lowerShadow <= (body * 0.4)) {
            return CandlestickPatternResult(
                CandlestickPattern.SHOOTING_STAR,
                false,
                "Shooting Star (Bearish Rejection): Sellers strongly rejected higher prices."
            )
        }

        // Morning Star (3-candle bullish reversal)
        if (priorClosed != null) {
            val isPriorRed = priorClosed.close < priorClosed.open
            val isPrevDojiOrSmall = abs(prevClosed.close - prevClosed.open) < (abs(priorClosed.close - priorClosed.open) * 0.4)
            val isLastGreen = lastClosed.close > lastClosed.open && lastClosed.close > (priorClosed.open + priorClosed.close) / 2.0
            if (isPriorRed && isPrevDojiOrSmall && isLastGreen) {
                return CandlestickPatternResult(
                    CandlestickPattern.MORNING_STAR,
                    true,
                    "Morning Star: 3-candle reversal confirming bottom exhaustion and buyer resurgence."
                )
            }

            // Evening Star (3-candle bearish reversal)
            val isPriorGreen = priorClosed.close > priorClosed.open
            val isLastRed = lastClosed.close < lastClosed.open && lastClosed.close < (priorClosed.open + priorClosed.close) / 2.0
            if (isPriorGreen && isPrevDojiOrSmall && isLastRed) {
                return CandlestickPatternResult(
                    CandlestickPattern.EVENING_STAR,
                    false,
                    "Evening Star: 3-candle reversal signaling buyer exhaustion and seller takeover."
                )
            }
        }

        // Inside Bar
        if (lastClosed.high < prevClosed.high && lastClosed.low > prevClosed.low) {
            return CandlestickPatternResult(
                CandlestickPattern.INSIDE_BAR,
                false,
                "Inside Bar: Price compression inside previous range. Breakout pending."
            )
        }

        return CandlestickPatternResult(CandlestickPattern.NONE, false, "No dominant pattern on last closed candle.")
    }

    /**
     * Detect RSI divergence over recent price swings
     */
    fun detectRSIDivergence(candles: List<CandleStick>): RSIDivergenceType {
        if (candles.size < 30) return RSIDivergenceType.NONE

        val closes = candles.map { it.close }
        val rsiCurrent = calculateRSI(closes, 14)
        val rsiPast = calculateRSI(closes.dropLast(6), 14)

        val priceCurrent = closes.last()
        val pricePast = closes[closes.size - 7]

        // Bearish divergence: price higher, RSI lower
        if (priceCurrent > pricePast * 1.015 && rsiCurrent < rsiPast - 4.0 && rsiCurrent > 58.0) {
            return RSIDivergenceType.BEARISH_DIVERGENCE
        }

        // Bullish divergence: price lower, RSI higher
        if (priceCurrent < pricePast * 0.985 && rsiCurrent > rsiPast + 4.0 && rsiCurrent < 42.0) {
            return RSIDivergenceType.BULLISH_DIVERGENCE
        }

        return RSIDivergenceType.NONE
    }
}
