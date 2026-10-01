package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.AIAnalysis
import com.example.model.AnalysisType
import com.example.model.CandleStick
import com.example.model.CryptoPair
import com.example.model.MarketTrend
import com.example.model.Ticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class GeminiApiService {

    private val tag = "AivoraGeminiApi"
    private val client = ApiClient.aiOkHttpClient

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Throwable) {
            ""
        }
    }

    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun analyzeLiveMarket(
        pair: CryptoPair,
        ticker: Ticker?,
        candles: List<CandleStick>
    ): Result<AIAnalysis> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is missing or not configured in AI Studio Secrets panel.")
            )
        }

        val prompt = buildLiveAnalysisPrompt(pair, ticker, candles)
        val model = "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val content = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    }
                    put("parts", parts)
                }
                put(content)
            }
            put("contents", contents)

            val genConfig = JSONObject().apply {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            }
            put("generationConfig", genConfig)
        }

        try {
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val code = response.code
                val err = parseApiError(responseString, code)
                return@withContext Result.failure(Exception("Gemini API error ($code): $err"))
            }

            val parsedText = extractCandidateText(responseString)
            if (parsedText.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Gemini API returned an empty response candidate."))
            }

            val analysis = parseAnalysisJson(parsedText, "${pair.displayName} (${pair.symbol}) Live Analysis", AnalysisType.LIVE_MARKET_DATA)
            Result.success(analysis)
        } catch (e: Exception) {
            Log.e(tag, "Live analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeScreenshot(
        bitmap: Bitmap,
        pairContext: String = "Cryptocurrency Chart"
    ): Result<AIAnalysis> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is missing or not configured in AI Studio Secrets panel.")
            )
        }

        val base64Image = bitmapToBase64(bitmap)
        val prompt = buildScreenshotPrompt(pairContext)
        val model = "gemini-3.1-pro-preview"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val content = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        put(JSONObject().apply {
                            val inlineData = JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            }
                            put("inlineData", inlineData)
                        })
                    }
                    put("parts", parts)
                }
                put(content)
            }
            put("contents", contents)

            val genConfig = JSONObject().apply {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            }
            put("generationConfig", genConfig)
        }

        try {
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val code = response.code
                val err = parseApiError(responseString, code)
                return@withContext Result.failure(Exception("Gemini Vision error ($code): $err"))
            }

            val parsedText = extractCandidateText(responseString)
            if (parsedText.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Gemini Vision returned an empty response candidate."))
            }

            val analysis = parseAnalysisJson(parsedText, "Local Screenshot Analysis", AnalysisType.LOCAL_SCREENSHOT)
            Result.success(analysis)
        } catch (e: Exception) {
            Log.e(tag, "Screenshot analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(IllegalStateException("No valid API Key found in BuildConfig."))
        }

        val model = "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val content = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", "Respond with exact JSON: {\"status\":\"ok\",\"service\":\"Aivora AI\"}"))
                    }
                    put("parts", parts)
                }
                put(content)
            }
            put("contents", contents)
        }

        try {
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Result.success("AI API Connected (HTTP 200 OK)")
            } else {
                val msg = parseApiError(responseString, response.code)
                Result.failure(Exception("HTTP ${response.code}: $msg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to reasonable dimensions and quality for quick upload
        val scaled = if (bitmap.width > 1280 || bitmap.height > 1280) {
            val ratio = minOf(1280f / bitmap.width, 1280f / bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else bitmap

        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun extractCandidateText(responseJson: String): String? {
        val root = JSONObject(responseJson)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val firstCand = candidates.getJSONObject(0)
        val content = firstCand.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        return parts.getJSONObject(0).optString("text")
    }

    private fun parseApiError(responseBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(responseBody)
            val error = json.optJSONObject("error")
            val message = error?.optString("message") ?: responseBody
            message
        } catch (e: Exception) {
            "HTTP $statusCode: $responseBody"
        }
    }

    private fun parseAnalysisJson(jsonText: String, title: String, type: AnalysisType): AIAnalysis {
        // Strip markdown code fences if model returned them
        val cleaned = jsonText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(cleaned)
        val trendStr = json.optString("trend", "NEUTRAL").uppercase()
        val trend = when {
            trendStr.contains("BULL") -> MarketTrend.BULLISH
            trendStr.contains("BEAR") -> MarketTrend.BEARISH
            trendStr.contains("CONSOLID") -> MarketTrend.CONSOLIDATION
            else -> MarketTrend.NEUTRAL
        }

        val confidence = json.optInt("confidencePercent", 75).coerceIn(10, 99)

        val patternsList = mutableListOf<String>()
        val patternsArr = json.optJSONArray("keyPatterns")
        if (patternsArr != null) {
            for (i in 0 until patternsArr.length()) {
                patternsList.add(patternsArr.getString(i))
            }
        }
        if (patternsList.isEmpty()) {
            patternsList.add("Price Range Equilibrium")
        }

        val supportList = mutableListOf<Double>()
        val supportArr = json.optJSONArray("supportLevels")
        if (supportArr != null) {
            for (i in 0 until supportArr.length()) {
                supportList.add(supportArr.optDouble(i))
            }
        }

        val resistanceList = mutableListOf<Double>()
        val resistanceArr = json.optJSONArray("resistanceLevels")
        if (resistanceArr != null) {
            for (i in 0 until resistanceArr.length()) {
                resistanceList.add(resistanceArr.optDouble(i))
            }
        }

        val indicators = json.optString("indicatorsSummary", "Moving Averages neutral, Volume normal.")
        val verdict = json.optString("technicalVerdict", "Market structure currently testing local support/resistance boundaries.")
        val risk = json.optString("riskAssessment", "Maintain tight risk boundaries. Wait for confirmed candle close before evaluating momentum.")

        return AIAnalysis(
            title = title,
            analysisType = type,
            trend = trend,
            confidencePercent = confidence,
            keyPatterns = patternsList,
            supportLevels = supportList,
            resistanceLevels = resistanceList,
            indicatorsSummary = indicators,
            technicalVerdict = verdict,
            riskAssessment = risk,
            timestamp = System.currentTimeMillis(),
            isDemo = false
        )
    }

    private fun buildLiveAnalysisPrompt(pair: CryptoPair, ticker: Ticker?, candles: List<CandleStick>): String {
        val lastPrice = ticker?.lastPrice ?: candles.lastOrNull()?.close ?: 0.0
        val change = ticker?.percentChange24h ?: 0.0
        val high = ticker?.highPrice24h ?: 0.0
        val low = ticker?.lowPrice24h ?: 0.0

        val closes = candles.map { it.close }
        val ema9 = com.example.data.engine.IndicatorCalculator.calculateEMA(closes, 9).lastOrNull() ?: lastPrice
        val ema21 = com.example.data.engine.IndicatorCalculator.calculateEMA(closes, 21).lastOrNull() ?: lastPrice
        val ema50 = com.example.data.engine.IndicatorCalculator.calculateEMA(closes, 50).lastOrNull() ?: ema21
        val rsi = com.example.data.engine.IndicatorCalculator.calculateRSI(closes, 14)
        val macd = com.example.data.engine.IndicatorCalculator.calculateMACD(closes)
        val structure = com.example.data.engine.IndicatorCalculator.analyzeMarketStructure(candles)
        val pattern = com.example.data.engine.IndicatorCalculator.detectPattern(candles)
        val sr = com.example.data.engine.IndicatorCalculator.calculateSupportResistance(candles)

        val candleSummary = candles.takeLast(10).joinToString(" | ") { c ->
            "O:${c.open}, H:${c.high}, L:${c.low}, C:${c.close}"
        }

        return """
            You are Aivora Chart AI, an elite professional crypto market technical analyst.
            Synthesize and explain the following REAL mathematical indicators calculated on live candles for ${pair.displayName} (${pair.symbol}):
            Current Price: $lastPrice
            24h Change: $change%
            24h Range: High $high / Low $low
            
            CALCULATED TECHNICAL METRICS (DO NOT INVENT OTHER NUMBERS):
            - RSI (14): ${String.format(java.util.Locale.US, "%.1f", rsi)}
            - EMA 9: ${String.format(java.util.Locale.US, "%.2f", ema9)} | EMA 21: ${String.format(java.util.Locale.US, "%.2f", ema21)} | EMA 50: ${String.format(java.util.Locale.US, "%.2f", ema50)}
            - MACD Histogram: ${String.format(java.util.Locale.US, "%+.4f", macd.histogram)} (Line: ${String.format(java.util.Locale.US, "%.4f", macd.macdLine)}, Signal: ${String.format(java.util.Locale.US, "%.4f", macd.signalLine)})
            - Key Support Zones: ${String.format(java.util.Locale.US, "%.2f", sr.s1)}, ${String.format(java.util.Locale.US, "%.2f", sr.s2)}
            - Key Resistance Zones: ${String.format(java.util.Locale.US, "%.2f", sr.r1)}, ${String.format(java.util.Locale.US, "%.2f", sr.r2)}
            - Market Structure: ${structure.structureSummary}
            - Last Closed Candle Pattern: ${pattern.description}
            - Recent 10 Candlestick closes: $candleSummary

            Explain these exact mathematical metrics clearly and synthesize a technical verdict based strictly on this calculated state.
            Strictly respond with ONLY valid JSON matching this schema:
            {
              "trend": "BULLISH" | "BEARISH" | "NEUTRAL" | "CONSOLIDATION",
              "confidencePercent": 85,
              "keyPatterns": ["Pattern 1", "Pattern 2"],
              "supportLevels": [double1, double2],
              "resistanceLevels": [double1, double2],
              "indicatorsSummary": "concise explanation of the above calculated RSI, EMA, and MACD values",
              "technicalVerdict": "clear 2-3 sentence technical overview synthesizing these exact numbers",
              "riskAssessment": "educational risk parameters and key invalidation zone based on calculated support/resistance"
            }
            Do not provide financial advice or trade order instructions. Educational analysis only.
        """.trimIndent()
    }

    private fun buildScreenshotPrompt(context: String): String {
        return """
            You are Aivora Chart AI, analyzing an uploaded cryptocurrency or financial chart screenshot.
            Context: $context.
            Carefully inspect:
            1. The asset, timeframe, and candlestick price action shown in the image.
            2. Visible trendlines, chart patterns (e.g. triangles, flags, double bottom/top, head and shoulders).
            3. Obvious support and resistance zones based on peaks and valleys.
            4. Any visible technical indicators (RSI, MACD, Moving Averages, Volume bars).

            Strictly respond with ONLY valid JSON with no preamble matching this schema:
            {
              "trend": "BULLISH" | "BEARISH" | "NEUTRAL" | "CONSOLIDATION",
              "confidencePercent": 82,
              "keyPatterns": ["Identified Pattern 1", "Identified Pattern 2"],
              "supportLevels": [double1, double2],
              "resistanceLevels": [double1, double2],
              "indicatorsSummary": "summary of visual indicators observed on chart",
              "technicalVerdict": "concise technical breakdown of the screenshot chart structure",
              "riskAssessment": "key price invalidation level and volatility considerations"
            }
            Do not provide financial advice or trade execution. Educational analysis only.
        """.trimIndent()
    }

    suspend fun askChatAssistant(
        question: String,
        pair: CryptoPair,
        ticker: Ticker?,
        candles: List<CandleStick>,
        signalSummary: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add GEMINI_API_KEY to AI Studio Secrets.")
            )
        }

        val prompt = buildChatPrompt(question, pair, ticker, candles, signalSummary)
        val model = "gemini-3.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val content = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    }
                    put(content)
                }
                put("contents", contents)
                val generationConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 600)
                }
                put("generationConfig", generationConfig)
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                return@withContext Result.failure(
                    Exception("Gemini API HTTP ${response.code}: $err")
                )
            }

            val respText = response.body?.string() ?: ""
            val json = JSONObject(respText)
            val candidates = json.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
            val answer = parts?.optJSONObject(0)?.optString("text") ?: "No analysis returned."
            Result.success(answer)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildChatPrompt(
        question: String,
        pair: CryptoPair,
        ticker: Ticker?,
        candles: List<CandleStick>,
        signalSummary: String
    ): String {
        val lastPrice = ticker?.lastPrice ?: candles.lastOrNull()?.close ?: 0.0
        val change24h = ticker?.percentChange24h ?: 0.0
        val candleSummary = candles.takeLast(8).joinToString(" | ") { c ->
            "O:${c.open}, H:${c.high}, L:${c.low}, C:${c.close}"
        }

        return """
            You are Aivora Chart AI, a professional technical chart assistant.
            Use ONLY the following real market data to answer the user's question accurately:
            Asset: ${pair.displayName} (${pair.coinbaseProduct})
            Current Price: $lastPrice
            24h Change: $change24h%
            Recent Candles: $candleSummary
            Calculated Signal & Indicators: $signalSummary

            User Question: "$question"

            Guidelines:
            - If asked "What is the current trend?", describe the trend based on the EMA 9 vs 21 and price action.
            - If asked "Why is this signal WAIT?", explain the conflicting indicators (e.g. RSI divergence or consolidating range).
            - If asked "Where is support?" or "Where is resistance?", cite the specific numeric levels from the data above.
            - If asked "What indicators confirm the signal?", refer directly to RSI, EMA 9/21, and MACD metrics.
            - Keep answers concise, factual, and strictly educational (no financial advice, no guaranteed predictions).
        """.trimIndent()
    }
}
