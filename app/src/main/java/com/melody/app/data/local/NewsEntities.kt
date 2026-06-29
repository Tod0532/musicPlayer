package com.melody.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 资讯收藏（用户标记感兴趣的资讯）
 */
@Entity(tableName = "news_favorites")
data class NewsFavoriteEntity(
    @PrimaryKey val newsId: String,    // NewsItem.id
    val title: String,
    val summary: String,
    val source: String,
    val url: String,
    val savedAt: Long = System.currentTimeMillis()
)

/**
 * 资讯播放历史（自动记录听过的）
 */
@Entity(tableName = "news_history")
data class NewsHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val newsId: String,
    val title: String,
    val source: String,
    val playedAt: Long = System.currentTimeMillis()
)
