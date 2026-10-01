package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {

    @Query("SELECT * FROM signals ORDER BY timestamp DESC")
    fun getAllSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE isSaved = 1 ORDER BY timestamp DESC")
    fun getSavedSignals(): Flow<List<SignalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: SignalEntity): Long

    @Query("DELETE FROM signals WHERE id = :id")
    suspend fun deleteSignalById(id: Long)

    @Query("DELETE FROM signals WHERE isSaved = 0")
    suspend fun clearScanHistory()

    @Query("DELETE FROM signals")
    suspend fun clearAllSignals()
}

@Dao
interface PriceAlertDao {

    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveAlerts(): Flow<List<PriceAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlertEntity): Long

    @Update
    suspend fun updateAlert(alert: PriceAlertEntity)

    @Query("UPDATE price_alerts SET isActive = 0, triggeredAt = :triggeredAt WHERE id = :id")
    suspend fun markAlertTriggered(id: Long, triggeredAt: Long)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Long)

    @Query("DELETE FROM price_alerts")
    suspend fun clearAllAlerts()
}

@Dao
interface PersonalPatternDao {

    @Query("SELECT * FROM personal_patterns ORDER BY createdAt DESC")
    fun getAllPatterns(): Flow<List<PersonalPatternEntity>>

    @Query("SELECT * FROM personal_patterns WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActivePatterns(): Flow<List<PersonalPatternEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: PersonalPatternEntity): Long

    @Update
    suspend fun updatePattern(pattern: PersonalPatternEntity)

    @Query("DELETE FROM personal_patterns WHERE id = :id")
    suspend fun deletePatternById(id: Long)

    @Query("DELETE FROM personal_patterns")
    suspend fun clearAllPatterns()
}

@Dao
interface CustomStrategyDao {

    @Query("SELECT * FROM custom_strategies ORDER BY createdAt DESC")
    fun getAllStrategies(): Flow<List<CustomStrategyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrategy(strategy: CustomStrategyEntity): Long

    @Update
    suspend fun updateStrategy(strategy: CustomStrategyEntity)

    @Query("DELETE FROM custom_strategies WHERE id = :id")
    suspend fun deleteStrategyById(id: Long)
}

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist_items WHERE watchlistName = :name ORDER BY displayOrder ASC, addedAt DESC")
    fun getWatchlist(name: String): Flow<List<WatchlistItemEntity>>

    @Query("SELECT * FROM watchlist_items ORDER BY addedAt DESC")
    fun getAllWatchlistItems(): Flow<List<WatchlistItemEntity>>

    @Query("SELECT * FROM watchlist_items WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<WatchlistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WatchlistItemEntity): Long

    @Query("DELETE FROM watchlist_items WHERE symbol = :symbol AND watchlistName = :name")
    suspend fun deleteItem(symbol: String, name: String)

    @Query("DELETE FROM watchlist_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("UPDATE watchlist_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
}
