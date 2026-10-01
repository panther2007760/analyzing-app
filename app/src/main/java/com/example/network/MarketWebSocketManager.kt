package com.example.network

import android.util.Log
import com.example.model.CryptoPair
import com.example.model.Ticker
import com.example.model.WebSocketConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

enum class WebSocketProvider {
    COINBASE,
    BINANCE
}

class MarketWebSocketManager(
    private val networkMonitor: NetworkMonitor,
    private val scope: CoroutineScope
) {
    private val tag = "AivoraWebSocket"
    private var webSocket: WebSocket? = null
    private var currentPair: CryptoPair = CryptoPair.DEFAULT_PAIRS.first()
    private val isExplicitlyDisconnected = AtomicBoolean(false)
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val connectionMutex = Mutex()

    // Default to COINBASE to prevent geo-blocking HTTP 451 errors
    private val _currentProvider = MutableStateFlow(WebSocketProvider.COINBASE)
    val currentProvider: StateFlow<WebSocketProvider> = _currentProvider.asStateFlow()

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    private val _tickerFlow = MutableSharedFlow<Ticker>(extraBufferCapacity = 64)
    val tickerFlow: SharedFlow<Ticker> = _tickerFlow.asSharedFlow()

    val messageCount = AtomicLong(0)
    private val _lastMessageTime = MutableStateFlow(0L)
    val lastMessageTime: StateFlow<Long> = _lastMessageTime.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    init {
        // Observe network monitor to pause/resume reconnection cleanly
        scope.launch {
            networkMonitor.networkStatus.collect { status ->
                if (status.isConnected) {
                    if (_connectionState.value == WebSocketConnectionState.OFFLINE_PAUSED ||
                        _connectionState.value == WebSocketConnectionState.DISCONNECTED
                    ) {
                        Log.i(tag, "Internet restored, triggering WebSocket reconnection...")
                        reconnectAttempts = 0
                        reconnect(immediate = true)
                    }
                } else {
                    Log.w(tag, "Device offline. Pausing WebSocket reconnection.")
                    cancelPendingReconnect()
                    _connectionState.value = WebSocketConnectionState.OFFLINE_PAUSED
                    safeCloseWebSocket()
                }
            }
        }
    }

    fun subscribePair(pair: CryptoPair) {
        val oldPair = currentPair
        currentPair = pair
        isExplicitlyDisconnected.set(false)

        val ws = webSocket
        if (ws != null && _connectionState.value == WebSocketConnectionState.CONNECTED) {
            if (_currentProvider.value == WebSocketProvider.COINBASE) {
                if (oldPair.coinbaseProduct != pair.coinbaseProduct) {
                    sendCoinbaseUnsubscribe(ws, oldPair.coinbaseProduct)
                    sendCoinbaseSubscribe(ws, pair.coinbaseProduct)
                }
                return
            }
        }

        // Only reconnect if not already connected or connecting
        if (_connectionState.value != WebSocketConnectionState.CONNECTED &&
            _connectionState.value != WebSocketConnectionState.CONNECTING
        ) {
            reconnect(immediate = true)
        }
    }

    fun disconnect() {
        isExplicitlyDisconnected.set(true)
        cancelPendingReconnect()
        safeCloseWebSocket()
        _connectionState.value = WebSocketConnectionState.DISCONNECTED
    }

    fun forceReconnect() {
        isExplicitlyDisconnected.set(false)
        reconnectAttempts = 0
        reconnect(immediate = true)
    }

    fun setProvider(provider: WebSocketProvider) {
        if (_currentProvider.value != provider) {
            _currentProvider.value = provider
            reconnectAttempts = 0
            reconnect(immediate = true)
        }
    }

    fun getWebSocketUrl(): String {
        return if (_currentProvider.value == WebSocketProvider.COINBASE) {
            ApiClient.DEFAULT_COINBASE_WS_URL
        } else {
            "${ApiClient.DEFAULT_BINANCE_WS_URL}/${currentPair.binanceSymbol.lowercase()}@ticker"
        }
    }

    private fun safeCloseWebSocket() {
        val oldWs = webSocket
        webSocket = null
        if (oldWs != null) {
            try {
                // Use cancel() to immediately terminate underlying socket and prevent trailing callbacks
                oldWs.cancel()
            } catch (e: Exception) {
                Log.w(tag, "Error closing websocket", e)
            }
        }
    }

    private fun cancelPendingReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    private fun reconnect(immediate: Boolean = false) {
        if (isExplicitlyDisconnected.get()) return

        val networkOnline = networkMonitor.networkStatus.value.isConnected
        if (!networkOnline) {
            _connectionState.value = WebSocketConnectionState.OFFLINE_PAUSED
            return
        }

        cancelPendingReconnect()

        reconnectJob = scope.launch(Dispatchers.IO) {
            if (!immediate && reconnectAttempts > 0) {
                val backoffMs = min(30000L, (1000L * (1L shl min(reconnectAttempts, 5))))
                val jitter = (Math.random() * 400).toLong()
                val totalDelay = backoffMs + jitter
                Log.d(tag, "Scheduling WebSocket reconnect in ${totalDelay}ms (attempt #$reconnectAttempts)")
                delay(totalDelay)
            }

            if (!networkMonitor.networkStatus.value.isConnected) {
                _connectionState.value = WebSocketConnectionState.OFFLINE_PAUSED
                return@launch
            }

            connectionMutex.withLock {
                connectInternal()
            }
        }
    }

    private fun connectInternal() {
        if (isExplicitlyDisconnected.get()) return

        safeCloseWebSocket()
        _connectionState.value = WebSocketConnectionState.CONNECTING

        val url = getWebSocketUrl()
        Log.i(tag, "Opening WebSocket (${_currentProvider.value}): $url")

        val request = Request.Builder()
            .url(url)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (webSocket !== this@MarketWebSocketManager.webSocket) {
                    Log.d(tag, "Ignoring onOpen from superseded WebSocket")
                    return
                }
                Log.i(tag, "WebSocket connected successfully to ${_currentProvider.value}")
                _connectionState.value = WebSocketConnectionState.CONNECTED
                reconnectAttempts = 0
                _lastError.value = null

                if (_currentProvider.value == WebSocketProvider.COINBASE) {
                    sendCoinbaseSubscribe(webSocket, currentPair.coinbaseProduct)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (webSocket !== this@MarketWebSocketManager.webSocket) return

                try {
                    messageCount.incrementAndGet()
                    _lastMessageTime.value = System.currentTimeMillis()

                    val json = JSONObject(text)

                    if (_currentProvider.value == WebSocketProvider.COINBASE) {
                        val msgType = json.optString("type")
                        if (msgType == "ticker") {
                            val prodId = json.optString("product_id")
                            val price = json.optDouble("price", 0.0)
                            val open24h = json.optDouble("open_24h", 0.0)
                            val high24h = json.optDouble("high_24h", 0.0)
                            val low24h = json.optDouble("low_24h", 0.0)
                            val volume24h = json.optDouble("volume_24h", 0.0)
                            val priceChange = if (open24h > 0) price - open24h else 0.0
                            val percentChange = if (open24h > 0) ((price - open24h) / open24h) * 100.0 else 0.0

                            if (price > 0.0) {
                                val ticker = Ticker(
                                    symbol = prodId,
                                    lastPrice = price,
                                    priceChange24h = priceChange,
                                    percentChange24h = percentChange,
                                    highPrice24h = high24h,
                                    lowPrice24h = low24h,
                                    volume24h = volume24h,
                                    timestamp = System.currentTimeMillis(),
                                    isLive = true
                                )
                                _tickerFlow.tryEmit(ticker)
                            }
                        }
                    } else {
                        // Binance format
                        val s = json.optString("s", currentPair.binanceSymbol)
                        val c = json.optDouble("c", 0.0)
                        val p = json.optDouble("p", 0.0)
                        val P = json.optDouble("P", 0.0)
                        val h = json.optDouble("h", 0.0)
                        val l = json.optDouble("l", 0.0)
                        val v = json.optDouble("v", 0.0)

                        if (c > 0.0) {
                            val ticker = Ticker(
                                symbol = s,
                                lastPrice = c,
                                priceChange24h = p,
                                percentChange24h = P,
                                highPrice24h = h,
                                lowPrice24h = l,
                                volume24h = v,
                                timestamp = System.currentTimeMillis(),
                                isLive = true
                            )
                            _tickerFlow.tryEmit(ticker)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Failed to parse websocket message: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (webSocket !== this@MarketWebSocketManager.webSocket) return
                Log.d(tag, "WebSocket onClosing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (webSocket !== this@MarketWebSocketManager.webSocket) return
                Log.i(tag, "WebSocket onClosed: $code $reason")
                if (!isExplicitlyDisconnected.get()) {
                    _connectionState.value = WebSocketConnectionState.DISCONNECTED
                    reconnectAttempts++
                    reconnect(immediate = false)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // If this failure is from an old, superseded or closed socket instance, ignore silently
                if (webSocket !== this@MarketWebSocketManager.webSocket) {
                    Log.d(tag, "Ignoring failure on superseded WebSocket instance: ${t.message}")
                    return
                }

                // If socket was closed programmatically, ignore
                val isSocketClosedMsg = t.message?.contains("Socket is closed", ignoreCase = true) == true
                if (isSocketClosedMsg || isExplicitlyDisconnected.get()) {
                    Log.d(tag, "WebSocket socket closed cleanly.")
                    return
                }

                val errorMsg = "WebSocket Failure: ${t.localizedMessage ?: t.javaClass.simpleName}"
                val responseCode = response?.code ?: 0
                Log.e(tag, "$errorMsg (HTTP $responseCode)")

                // Auto-fallback if HTTP 451 (Geo-blocked by Binance) or 403
                if (responseCode == 451 || t.message?.contains("451") == true) {
                    Log.w(tag, "HTTP 451 Geo-restriction on ${_currentProvider.value}. Switching to Coinbase global feed...")
                    _lastError.value = "HTTP 451: Geo-restricted endpoint. Switched to Coinbase."
                    _currentProvider.value = WebSocketProvider.COINBASE
                    reconnectAttempts = 0
                    reconnect(immediate = true)
                    return
                }

                _lastError.value = errorMsg

                val isOffline = !networkMonitor.networkStatus.value.isConnected
                if (isOffline) {
                    _connectionState.value = WebSocketConnectionState.OFFLINE_PAUSED
                } else {
                    _connectionState.value = WebSocketConnectionState.ERROR
                    if (!isExplicitlyDisconnected.get()) {
                        reconnectAttempts++
                        reconnect(immediate = false)
                    }
                }
            }
        }

        webSocket = ApiClient.okHttpClient.newWebSocket(request, listener)
    }

    private fun sendCoinbaseSubscribe(ws: WebSocket, productId: String) {
        try {
            val json = JSONObject().apply {
                put("type", "subscribe")
                val pids = JSONArray().apply { put(productId) }
                put("product_ids", pids)
                val channels = JSONArray().apply {
                    put("ticker")
                }
                put("channels", channels)
            }
            ws.send(json.toString())
            Log.d(tag, "Sent Coinbase subscribe for $productId")
        } catch (e: Exception) {
            Log.w(tag, "Failed to send subscribe: ${e.message}")
        }
    }

    private fun sendCoinbaseUnsubscribe(ws: WebSocket, productId: String) {
        try {
            val json = JSONObject().apply {
                put("type", "unsubscribe")
                val pids = JSONArray().apply { put(productId) }
                put("product_ids", pids)
                val channels = JSONArray().apply { put("ticker") }
                put("channels", channels)
            }
            ws.send(json.toString())
        } catch (e: Exception) {
            Log.w(tag, "Failed to send unsubscribe: ${e.message}")
        }
    }
}
