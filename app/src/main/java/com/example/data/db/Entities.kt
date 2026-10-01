package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pairSymbol: String,
    val timestamp: Long = System.currentTimeMillis(),
    val bias: String,
    val strength: String,
    val currentPrice: Double,
    val entryZone: String,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val riskRewardRatio: String,
    val rsi: Double,
    val ema9: Double,
    val ema21: Double,
    val reasonsSummary: String,
    val invalidation: String,
    val isSaved: Boolean = true
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pairSymbol: String,
    val targetPrice: Double,
    val isAbove: Boolean,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val triggeredAt: Long? = null
)

@Entity(tableName = "personal_patterns")
data class PersonalPatternEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val market: String, // CRYPTO, FOREX, STOCKS, INDICES, COMMODITIES
    val asset: String,  // e.g. "BTC-USD", "EUR/USD"
    val timeframe: String,
    val direction: String, // "LONG" or "SHORT"
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val riskRewardRatio: Double,
    val rsiValue: Double,
    val emaAlignment: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val candlestickPattern: String,
    val marketStructure: String,
    val entryReason: String,
    val result: String = "WIN", // "WIN", "LOSS", "BREAK_EVEN", "PENDING"
    val profitPercent: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "custom_strategies")
data class CustomStrategyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val market: String = "CRYPTO",
    val timeframe: String = "1H",
    val direction: String = "LONG",
    val minRsi: Double = 40.0,
    val maxRsi: Double = 65.0,
    val requireEmaAlignment: Boolean = true,
    val requireMacdConfirmation: Boolean = true,
    val targetRR: Double = 2.0,
    val sampleCount: Int = 0,
    val winCount: Int = 0,
    val lossCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "watchlist_items")
data class WatchlistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val watchlistName: String = "Default",
    val symbol: String,
    val name: String,
    val marketType: String = "CRYPTO",
    val displayOrder: Int = 0,
    val isFavorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
