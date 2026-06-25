package com.melody.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 歌单实体
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 歌单-歌曲 关联实体
 */
@Entity(
    tableName = "playlist_song",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("songId")]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,        // 对应 Song.id
    val addedAt: Long = System.currentTimeMillis(),
    // 冗余存储歌曲信息（简化查询，避免 JOIN 复杂度）
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val coverColor: Long,
    val coverColor2: Long,
    val audioAsset: String? = null,
    val mediaUri: String? = null
)
