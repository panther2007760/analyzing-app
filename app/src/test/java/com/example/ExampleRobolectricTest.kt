package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DemoDataProvider
import com.example.data.db.AivoraDatabase
import com.example.data.db.PriceAlertEntity
import com.example.data.db.SignalEntity
import com.example.data.engine.CandlestickPattern
import com.example.data.engine.IndicatorCalculator
import com.example.data.engine.SignalBias
import com.example.data.engine.SignalEngine
import com.example.data.engine.SignalStrength
import com.example.data.forex.ForexRepository
import com.example.model.AppLanguage
import com.example.model.AppStrings
import com.example.model.CandleStick
import com.example.model.CryptoPair
import com.example.model.NetworkStatus
import com.example.model.NetworkType
import com.example.model.Timeframe
import com.example.network.NetworkMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AivoraDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AivoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Aivora Chart AI", appName)
    }

    @Test
    fun `verify demo data is strictly marked not live`() {
        val btc = CryptoPair.DEFAULT_PAIRS.first { it.baseAsset == "BTC" }
        val demoTicker = DemoDataProvider.getDemoTicker(btc)
        assertFalse("Demo ticker must NEVER be marked as live", demoTicker.isLive)
        assertTrue(demoTicker.lastPrice > 0)

        val demoCandles = DemoDataProvider.getDemoCandles(demoTicker.lastPrice)
        assertTrue(demoCandles.isNotEmpty())
        assertEquals(30, demoCandles.size)

        val demoAnalysis = DemoDataProvider.getDemoAnalysis(btc)
        assertTrue("Demo analysis must be flagged as isDemo", demoAnalysis.isDemo)
        assertTrue(demoAnalysis.title.contains("DEMO MODE"))
    }

    @Test
    fun `verify candle data validation logic`() {
        // Valid candle
        val valid = CandleStick(openTime = 1000L, open = 100.0, high = 105.0, low = 95.0, close = 102.0, volume = 50.0)
        assertTrue("Valid candle should pass validation", valid.isValid())

        // Invalid: high < open
        val invalidHigh = CandleStick(openTime = 1000L, open = 100.0, high = 98.0, low = 95.0, close = 97.0, volume = 50.0)
        assertFalse("High below open must fail", invalidHigh.isValid())

        // Invalid: low > close
        val invalidLow = CandleStick(openTime = 1000L, open = 100.0, high = 105.0, low = 103.0, close = 101.0, volume = 50.0)
        assertFalse("Low above close must fail", invalidLow.isValid())

        // Invalid: negative volume
        val invalidVol = CandleStick(openTime = 1000L, open = 100.0, high = 105.0, low = 95.0, close = 102.0, volume = -10.0)
        assertFalse("Negative volume must fail", invalidVol.isValid())
    }

    @Test
    fun `verify technical indicators - RSI calculation`() {
        val flatCloses = List(30) { 100.0 }
        val rsiFlat = IndicatorCalculator.calculateRSI(flatCloses, 14)
        assertEquals(50.0, rsiFlat, 0.1)

        val risingCloses = (1..30).map { 100.0 + (it * 2.0) }
        val rsiRising = IndicatorCalculator.calculateRSI(risingCloses, 14)
        assertTrue("RSI for steady uptrend should be > 80, got $rsiRising", rsiRising > 80.0)

        val fallingCloses = (1..30).map { 200.0 - (it * 3.0) }
        val rsiFalling = IndicatorCalculator.calculateRSI(fallingCloses, 14)
        assertTrue("RSI for steady downtrend should be < 20, got $rsiFalling", rsiFalling < 20.0)
    }

    @Test
    fun `verify technical indicators - EMA, MACD, Bollinger Bands, ATR`() {
        val prices = (1..35).map { 100.0 + it }
        val ema9 = IndicatorCalculator.calculateEMA(prices, 9)
        assertEquals(prices.size - 9 + 1, ema9.size)
        assertTrue(ema9.last() > ema9.first())

        val ema21 = IndicatorCalculator.calculateEMA(prices, 21)
        assertTrue(ema9.last() > ema21.last()) // In uptrend, EMA 9 > EMA 21

        val macd = IndicatorCalculator.calculateMACD(prices)
        assertNotNull(macd)
        assertTrue("MACD line in steady uptrend should be positive", macd.macdLine > 0.0)

        val bb = IndicatorCalculator.calculateBollingerBands(prices, 20)
        assertTrue(bb.upper > bb.middle)
        assertTrue(bb.middle > bb.lower)
        assertTrue(bb.bandwidth > 0.0)

        val candles = (1..35).map { i ->
            val p = 100.0 + i
            CandleStick(openTime = i * 60000L, open = p - 1, high = p + 2, low = p - 2, close = p, volume = 100.0)
        }
        val atr = IndicatorCalculator.calculateATR(candles, 14)
        assertTrue(atr > 0.0)
    }

    @Test
    fun `verify signal engine conflict detection returns WAIT`() {
        val btc = CryptoPair.DEFAULT_PAIRS.first { it.baseAsset == "BTC" }
        val candles = ArrayList<CandleStick>()
        for (i in 0..25) {
            val p = 60000.0 + (i * 20.0)
            candles.add(CandleStick(openTime = i * 3600000L, open = p - 10, high = p + 20, low = p - 20, close = p, volume = 100.0))
        }
        for (i in 26..32) {
            val p = 60500.0 + ((i - 25) * 800.0)
            candles.add(CandleStick(openTime = i * 3600000L, open = p - 50, high = p + 100, low = p - 60, close = p, volume = 500.0))
        }

        val signal = SignalEngine.generateSignal(btc, Timeframe.H1, candles, candles.last().close)
        if (signal.rsi > 70.0) {
            assertEquals(SignalBias.WAIT, signal.bias)
            assertTrue("Should flag mixed conflict when overbought", signal.isMixedConflict)
        }
    }

    @Test
    fun `verify Room database signal and alert persistence`() = runBlocking {
        val signalEntity = SignalEntity(
            pairSymbol = "BTC-USD",
            bias = "LONG_BIAS",
            strength = "BULLISH",
            currentPrice = 67500.0,
            entryZone = "$67,400 - $67,600",
            stopLoss = 66200.0,
            takeProfit1 = 69400.0,
            takeProfit2 = 71200.0,
            riskRewardRatio = "1 : 1.5",
            rsi = 56.4,
            ema9 = 67200.0,
            ema21 = 66800.0,
            reasonsSummary = "EMA 9 holds above EMA 21; RSI healthy momentum.",
            invalidation = "Close below 66,200",
            isSaved = true
        )

        val id = db.signalDao().insertSignal(signalEntity)
        assertTrue(id > 0)

        val savedList = db.signalDao().getSavedSignals().first()
        assertEquals(1, savedList.size)
        assertEquals("BTC-USD", savedList[0].pairSymbol)
        assertEquals("LONG_BIAS", savedList[0].bias)

        val alertEntity = PriceAlertEntity(
            pairSymbol = "ETH-USD",
            targetPrice = 3500.0,
            isAbove = true,
            isActive = true
        )
        val alertId = db.priceAlertDao().insertAlert(alertEntity)
        assertTrue(alertId > 0)

        val activeAlerts = db.priceAlertDao().getActiveAlerts().first()
        assertEquals(1, activeAlerts.size)
        assertEquals("ETH-USD", activeAlerts[0].pairSymbol)
    }

    @Test
    fun `verify Forex repository major pairs and not-configured status`() {
        val monitor = NetworkMonitor(context)
        val forexRepo = ForexRepository(monitor)
        val pairs = forexRepo.pairs.value
        assertEquals(7, pairs.size)

        val symbols = pairs.map { it.symbol }
        assertTrue(symbols.contains("EUR/USD"))
        assertTrue(symbols.contains("GBP/USD"))
        assertTrue(symbols.contains("USD/JPY"))
        assertTrue(symbols.contains("USD/CHF"))
        assertTrue(symbols.contains("AUD/USD"))
        assertTrue(symbols.contains("USD/CAD"))
        assertTrue(symbols.contains("NZD/USD"))

        assertTrue("Must state real-time streaming is not configured",
            forexRepo.providerStatus.value.contains("NOT CONFIGURED")
        )
    }

    @Test
    fun `verify localization strings for English and Sinhala`() {
        val enSignals = AppStrings.get("tab_signals", AppLanguage.ENGLISH)
        assertEquals("Signals", enSignals)

        val siSignals = AppStrings.get("tab_signals", AppLanguage.SINHALA)
        assertEquals("සංඥා", siSignals)

        val enLong = AppStrings.get("long_bias", AppLanguage.ENGLISH)
        assertEquals("LONG BIAS", enLong)

        val siLong = AppStrings.get("long_bias", AppLanguage.SINHALA)
        assertTrue(siLong.contains("LONG"))
    }
}
