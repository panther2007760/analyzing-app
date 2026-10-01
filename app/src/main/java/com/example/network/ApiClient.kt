package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val TAG = "AivoraApiClient"

    // Primary global feed (Coinbase: zero geo-blocking, 100% US & global reliability, no 451 errors)
    const val DEFAULT_COINBASE_BASE_URL = "https://api.exchange.coinbase.com/"
    const val DEFAULT_COINBASE_WS_URL = "wss://ws-feed.exchange.coinbase.com"

    // Fallback/alternative feed
    const val DEFAULT_BINANCE_BASE_URL = "https://api.binance.com/"
    const val DEFAULT_BINANCE_WS_URL = "wss://stream.binance.com:9443/ws"

    private val modernTlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3)
        .build()

    private val userAgentInterceptor = Interceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("User-Agent", "AivoraChartAI/1.0 (Android; Mobile)")
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor { message ->
            if (BuildConfig.DEBUG) {
                val sanitized = if (message.contains("key=")) {
                    message.replace(Regex("key=[a-zA-Z0-9_-]+"), "key=REDACTED")
                } else message
                Log.d(TAG, sanitized)
            }
        }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS) // Automatic WebSocket ping-pong
            .connectionSpecs(listOf(modernTlsSpec, ConnectionSpec.COMPATIBLE_TLS))
            .retryOnConnectionFailure(true)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val aiOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .connectionSpecs(listOf(modernTlsSpec, ConnectionSpec.COMPATIBLE_TLS))
            .retryOnConnectionFailure(true)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun createRetrofit(baseUrl: String): Retrofit {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun getMarketService(baseUrl: String = DEFAULT_COINBASE_BASE_URL): MarketApiService {
        return createRetrofit(baseUrl).create(MarketApiService::class.java)
    }
}
