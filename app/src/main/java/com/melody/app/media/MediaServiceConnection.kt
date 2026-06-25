package com.melody.app.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * 媒体服务连接管理器
 *
 * 管理 UI 层与 PlaybackService 之间的 MediaController 连接。
 * UI 通过这个控制器操作 Service 里的播放器（实现后台播放）。
 */
class MediaServiceConnection(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    // 状态变化回调
    var onConnected: (() -> Unit)? = null
    var onPlaybackStateChanged: ((isPlaying: Boolean, position: Long, duration: Long) -> Unit)? = null
    var onPlaybackCompleted: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val p = controller ?: return
            onPlaybackStateChanged?.invoke(isPlaying, p.currentPosition, p.duration.coerceAtLeast(0))
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                onPlaybackCompleted?.invoke()
            } else if (state == Player.STATE_READY) {
                val p = controller ?: return
                onPlaybackStateChanged?.invoke(p.isPlaying, p.currentPosition, p.duration.coerceAtLeast(0))
            }
        }
    }

    /**
     * 连接到 PlaybackService
     */
    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync().also { future ->
            future.addListener({
                controller = future.get()
                controller?.addListener(playerListener)
                onConnected?.invoke()
            }, MoreExecutors.directExecutor())
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    val isConnected: Boolean get() = controller != null

    /**
     * 播放指定音频（assets 文件）
     */
    fun playAsset(assetFileName: String) {
        val c = controller ?: return
        c.setMediaItem(MediaItem.fromUri("asset:///$assetFileName"))
        c.prepare()
        c.play()
    }

    /**
     * 播放指定 URI（content:// 或 https://）
     */
    fun playUri(uriString: String) {
        val c = controller ?: return
        c.setMediaItem(MediaItem.fromUri(uriString))
        c.prepare()
        c.play()
    }

    fun play() { controller?.play() }
    fun pause() { controller?.pause() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }
    fun stop() { controller?.stop() }

    fun getCurrentPosition(): Long = controller?.currentPosition ?: 0
    fun getDuration(): Long = controller?.duration?.coerceAtLeast(0) ?: 0
    fun isPlaying(): Boolean = controller?.isPlaying ?: false
}
