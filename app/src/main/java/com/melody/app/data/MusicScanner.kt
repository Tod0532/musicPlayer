package com.melody.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.melody.app.domain.model.CoverColors
import com.melody.app.domain.model.Song

/**
 * MediaStore 扫描器：读取设备本地音乐
 */
object MusicScanner {

    // 封面色板循环（扫描到的歌曲轮流分配，让每首歌封面颜色不同）
    private val coverPalette = listOf(
        CoverColors.GRADIENT_BLUE to CoverColors.GRADIENT_PURPLE,
        CoverColors.GRADIENT_PINK to CoverColors.GRADIENT_ORANGE,
        CoverColors.GRADIENT_GREEN to CoverColors.GRADIENT_CYAN,
        CoverColors.GRADIENT_YELLOW to CoverColors.GRADIENT_RED,
        CoverColors.GRADIENT_PURPLE to CoverColors.GRADIENT_PINK,
        CoverColors.GRADIENT_ORANGE to CoverColors.GRADIENT_YELLOW
    )

    /**
     * 扫描设备本地音乐
     * @return 扫描到的歌曲列表（可能为空，表示无权限或无音乐文件）
     */
    fun scan(context: Context): List<Song> {
        val result = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // 只查询音乐（IS_MUSIC = 1），排除铃声/通知音等
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor = try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )
        } catch (e: Exception) {
            System.err.println("MELODY_SCAN: 扫描失败 - ${e.message}")
            return emptyList()
        }

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            // ALBUM_ID 可能在某些设备不可用，用 getColumnIndex 安全读取
            val albumIdColumn = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

            var index = 0
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: "未知歌曲"
                val artist = it.getString(artistColumn) ?: "未知歌手"
                val album = it.getString(albumColumn) ?: "未知专辑"
                val duration = it.getLong(durationColumn)
                val contentUri = ContentUris.withAppendedId(
                    collection, id
                )
                // 专辑封面 URI（用 ALBUM_ID 拼接 Artwork 的 contentUri）
                val albumId = if (albumIdColumn >= 0) it.getLong(albumIdColumn) else -1L
                val coverUri = if (albumId > 0) {
                    "content://media/external/audio/albumart/$albumId"
                } else null

                // 跳过过短的音频（< 10 秒，可能是铃声片段）
                if (duration < 10000) continue

                val colors = coverPalette[index % coverPalette.size]
                result.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        coverColor = colors.first,
                        coverColor2 = colors.second,
                        mediaUri = contentUri.toString(),  // 真实歌曲的 content:// URI
                        coverUri = coverUri                // 专辑封面 URI
                    )
                )
                index++
                // 限制最多 200 首，避免过多
                if (index >= 200) break
            }
        }

        System.err.println("MELODY_SCAN: 扫描到 ${result.size} 首本地音乐")
        return result
    }

    /**
     * 根据 id 获取歌曲的 contentUri（用于 Media3 播放）
     */
    fun getUriForId(id: Long): Uri {
        return ContentUris.withAppendedId(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), id
        )
    }
}
