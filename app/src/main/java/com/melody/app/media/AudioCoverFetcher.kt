package com.melody.app.media

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

/**
 * 音频封面提取器
 *
 * 用 MediaMetadataRetriever 从音频文件提取内嵌专辑封面（EMBEDDED_PICTURE）。
 * 解决某些 mp3 文件封面未被系统 MediaStore 索引、albumart URI 报错的问题。
 */
class AudioCoverFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val coverBytes = retriever.embeddedPicture
            retriever.release()

            System.err.println("MELODY_COVER: 提取 ${uri.lastPathSegment} -> ${coverBytes?.size ?: 0} 字节")

            if (coverBytes == null || coverBytes.isEmpty()) {
                return null
            }

            val bitmap = BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.size)
            if (bitmap == null) {
                System.err.println("MELODY_COVER: 字节解码失败")
                return null
            }
            System.err.println("MELODY_COVER: 成功 ${bitmap.width}x${bitmap.height}")

            DrawableResult(
                drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap),
                isSampled = false,
                dataSource = DataSource.DISK
            )
        } catch (e: Exception) {
            System.err.println("MELODY_COVER: 异常 ${e.message}")
            try { retriever.release() } catch (_: Exception) {}
            null
        }
    }

    /**
     * 工厂：匹配自定义 scheme "audio-cover://<真实URI>"
     * 用自定义 scheme 避免被 Coil 默认的 ContentUriFetcher 抢先处理
     */
    class Factory : Fetcher.Factory<Any> {
        override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
            val str = when (data) {
                is Uri -> data.toString()
                is String -> data
                else -> return null
            }
            // 只处理自定义 scheme audio-cover://
            return if (str.startsWith("audio-cover://")) {
                // 提取真实 URI（去掉 scheme 前缀）
                val realUriStr = str.removePrefix("audio-cover://")
                AudioCoverFetcher(options.context, Uri.parse(realUriStr))
            } else {
                null
            }
        }
    }
}
