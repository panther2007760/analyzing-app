package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SignalEntity::class,
        PriceAlertEntity::class,
        PersonalPatternEntity::class,
        CustomStrategyEntity::class,
        WatchlistItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AivoraDatabase : RoomDatabase() {

    abstract fun signalDao(): SignalDao
    abstract fun priceAlertDao(): PriceAlertDao
    abstract fun personalPatternDao(): PersonalPatternDao
    abstract fun customStrategyDao(): CustomStrategyDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: AivoraDatabase? = null

        fun getDatabase(context: Context): AivoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AivoraDatabase::class.java,
                    "aivora_chart_ai.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
