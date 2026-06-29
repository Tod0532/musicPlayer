package com.melody.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 应用数据库
 */
@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        NewsFavoriteEntity::class,
        NewsHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MelodyDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun newsDao(): NewsDao

    companion object {
        @Volatile
        private var INSTANCE: MelodyDatabase? = null

        fun getInstance(context: Context): MelodyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MelodyDatabase::class.java,
                    "melody.db"
                ).fallbackToDestructiveMigration().build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
