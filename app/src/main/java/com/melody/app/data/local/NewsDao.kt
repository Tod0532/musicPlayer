package com.melody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 资讯数据访问（收藏 + 历史）
 */
@Dao
interface NewsDao {

    // ---- 收藏 ----

    @Query("SELECT * FROM news_favorites ORDER BY savedAt DESC")
    fun observeFavorites(): Flow<List<NewsFavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: NewsFavoriteEntity)

    @Query("DELETE FROM news_favorites WHERE newsId = :newsId")
    suspend fun removeFavorite(newsId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM news_favorites WHERE newsId = :newsId)")
    suspend fun isFavorite(newsId: String): Boolean

    // ---- 历史 ----

    @Query("SELECT * FROM news_history ORDER BY playedAt DESC LIMIT 100")
    fun observeHistory(): Flow<List<NewsHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(history: NewsHistoryEntity)

    @Query("DELETE FROM news_history")
    suspend fun clearHistory()
}
