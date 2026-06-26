package com.melody.app.media

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri

/**
 * 直接从音频文件提取内嵌封面 bitmap（同步调用，需在 IO 线程）
 *
 * @param coverUri 格式 "audio-cover://<真实音频URI>" 或直接音频 URI
 * @return 封面 bitmap，无封面返回 null
 */
fun extractEmbeddedCover(context: Context, coverUri: String): android.graphics.Bitmap? {
    // 解析自定义 scheme
    val realUriStr = if (coverUri.startsWith("audio-cover://")) {
        coverUri.removePrefix("audio-cover://")
    } else {
        coverUri
    }
    val uri = Uri.parse(realUriStr) ?: return null

    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val bytes = retriever.embeddedPicture
        retriever.release()
        if (bytes == null || bytes.isEmpty()) {
            null
        } else {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    } catch (e: Exception) {
        try { retriever.release() } catch (_: Exception) {}
        null
    }
}
