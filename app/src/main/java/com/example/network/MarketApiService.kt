package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// --- Coinbase Models ---
@JsonClass(generateAdapter = true)
data class CoinbaseStatsResponse(
    @param:Json(name = "open") val open: String? = "0.0",
    @param:Json(name = "high") val high: String? = "0.0",
    @param:Json(name = "low") val low: String? = "0.0",
    @param:Json(name = "last") val last: String? = "0.0",
    @param:Json(name = "volume") val volume: String? = "0.0"
)

@JsonClass(generateAdapter = true)
data class CoinbaseTickerResponse(
    @param:Json(name = "price") val price: String? = "0.0",
    @param:Json(name = "volume") val volume: String? = "0.0",
    @param:Json(name = "time") val time: String? = ""
)

@JsonClass(generateAdapter = true)
data class CoinbaseTimeResponse(
    @param:Json(name = "epoch") val epoch: Double? = 0.0,
    @param:Json(name = "iso") val iso: String? = ""
)

// --- Binance Models ---
@JsonClass(generateAdapter = true)
data class BinanceTicker24hResponse(
    @param:Json(name = "symbol") val symbol: String,
    @param:Json(name = "priceChange") val priceChange: String? = "0.0",
    @param:Json(name = "priceChangePercent") val priceChangePercent: String? = "0.0",
    @param:Json(name = "lastPrice") val lastPrice: String? = "0.0",
    @param:Json(name = "highPrice") val highPrice: String? = "0.0",
    @param:Json(name = "lowPrice") val lowPrice: String? = "0.0",
    @param:Json(name = "volume") val volume: String? = "0.0",
    @param:Json(name = "closeTime") val closeTime: Long? = 0L
)

@JsonClass(generateAdapter = true)
data class BinanceServerTimeResponse(
    @param:Json(name = "serverTime") val serverTime: Long
)

interface MarketApiService {

    // --- Coinbase Endpoints (Globally available, no geo-blocking 451) ---
    @GET("time")
    suspend fun getCoinbaseTime(): Response<CoinbaseTimeResponse>

    @GET("products/{productId}/stats")
    suspend fun getCoinbaseStats(
        @Path("productId") productId: String
    ): Response<CoinbaseStatsResponse>

    @GET("products/{productId}/ticker")
    suspend fun getCoinbaseTicker(
        @Path("productId") productId: String
    ): Response<CoinbaseTickerResponse>

    @GET("products/{productId}/candles")
    suspend fun getCoinbaseCandles(
        @Path("productId") productId: String,
        @Query("granularity") granularity: Int = 3600
    ): Response<List<List<Double>>>

    // --- Binance Endpoints ---
    @GET("api/v3/ping")
    suspend fun ping(): Response<ResponseBody>

    @GET("api/v3/time")
    suspend fun getServerTime(): Response<BinanceServerTimeResponse>

    @GET("api/v3/ticker/24hr")
    suspend fun get24hTicker(
        @Query("symbol") symbol: String
    ): Response<BinanceTicker24hResponse>

    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "1h",
        @Query("limit") limit: Int = 60
    ): Response<List<List<Any>>>
}
