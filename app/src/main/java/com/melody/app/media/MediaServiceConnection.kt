package com.melody.app.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/**
 * 媒体服务连接管理器
 *
 * 管理 UI 层与 PlaybackService 之间的 MediaController 连接。
 * 核心改进：支持设置完整播放队列，让通知栏/锁屏自动显示上一首/下一首按钮。
 */
class MediaServiceConnection(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    var onConnected: (() -> Unit)? = null
    var onPlaybackStateChanged: ((isPlaying: Boolean, position: Long, duration: Long) -> Unit)? = null
    var onPlaybackCompleted: (() -> Unit)? = null
    var onError: ((message: String) -> Unit)? = null
    /** 队列切换（上一首/下一首）时回调，参数是新的 index */
    var onQueueIndexChanged: ((newIndex: Int) -> Unit)? = null

    // 待执行的请求（Service 未连接时暂存）
    private var pendingPlay: (() -> Unit)? = null
    // 当前队列的 URI 列表（与 Player 队列一一对应）
    private var queueUris: List<String> = emptyList()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val p = controller ?: return
            onPlaybackStateChanged?.invoke(isPlaying, p.currentPosition, p.duration.coerceAtLeast(0))
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_ENDED -> onPlaybackCompleted?.invoke()
                Player.STATE_READY -> {
                    val p = controller ?: return
                    onPlaybackStateChanged?.invoke(p.isPlaying, p.currentPosition, p.duration.coerceAtLeast(0))
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // 队列内自动切换（下一首/上一首触发），通知 UI 更新当前歌曲
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                val p = controller ?: return
                onQueueIndexChanged?.invoke(p.currentMediaItemIndex)
            }
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync().also { future ->
            future.addListener({
                controller = try {
                    future.get()
                } catch (e: Exception) {
                    onError?.invoke("播放服务连接失败: ${e.message}")
                    return@addListener
                }
                controller?.addListener(playerListener)
                onConnected?.invoke()
                pendingPlay?.invoke()
                pendingPlay = null
            }, MoreExecutors.directExecutor())
        }
    }

    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    val isConnected: Boolean get() = controller != null

    /**
     * 设置完整播放队列并从指定索引开始播放
     *
     * 这样 Player 持有多首歌曲，系统通知栏/锁屏会自动显示上一首/下一首按钮，
     * 点击时 Player 自动 seekToNext/seekToPrevious。
     *
     * @param items 队列（每项含 uri + 标题/歌手，用于通知栏显示）
     * @param startIndex 从第几首开始
     */
    fun setQueueAndPlay(items: List<QueueItem>, startIndex: Int) {
        val action: () -> Unit = {
            controller?.apply {
                if (items.isEmpty()) return@apply
                val mediaItems = items.map { it.toMediaItem() }
                queueUris = items.map { it.uri }
                setMediaItems(mediaItems, startIndex.coerceIn(0, items.lastIndex), 0L)
                prepare()
                play()
            }
        }
        if (controller != null) action() else pendingPlay = action
    }

    fun playAsset(assetFileName: String) {
        val action: () -> Unit = {
            controller?.apply {
                setMediaItem(MediaItem.fromUri("asset:///$assetFileName"))
                prepare()
                play()
            }
        }
        if (controller != null) action() else pendingPlay = action
    }

    fun playUri(uriString: String) {
        val action: () -> Unit = {
            controller?.apply {
                setMediaItem(MediaItem.fromUri(uriString))
                prepare()
                play()
            }
        }
        if (controller != null) action() else pendingPlay = action
    }

    fun play() { controller?.play() }
    fun pause() { controller?.pause() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }
    fun stop() { controller?.stop() }

    /** 通知栏/锁屏点击下一首（也可由 UI 调用） */
    fun seekToNext() { controller?.seekToNext() }
    /** 通知栏/锁屏点击上一首（也可由 UI 调用） */
    fun seekToPrevious() { controller?.seekToPrevious() }

    /**
     * 设置播放模式（同步到 Player，影响通知栏循环/随机行为）
     * - SEQUENCE: 顺序播放，播完队列停止
     * - SHUFFLE: 随机播放
     * - REPEAT_ONE: 单曲循环
     */
    fun setPlayMode(mode: com.melody.app.domain.model.PlayMode) {
        val p = controller ?: return
        when (mode) {
            com.melody.app.domain.model.PlayMode.SEQUENCE -> {
                p.shuffleModeEnabled = false
                p.repeatMode = Player.REPEAT_MODE_OFF
            }
            com.melody.app.domain.model.PlayMode.SHUFFLE -> {
                p.shuffleModeEnabled = true
                p.repeatMode = Player.REPEAT_MODE_ALL
            }
            com.melody.app.domain.model.PlayMode.REPEAT_ONE -> {
                p.shuffleModeEnabled = false
                p.repeatMode = Player.REPEAT_MODE_ONE
            }
        }
    }

    fun getCurrentPosition(): Long = controller?.currentPosition ?: 0
    fun getDuration(): Long = controller?.duration?.coerceAtLeast(0) ?: 0
    fun isPlaying(): Boolean = controller?.isPlaying ?: false
}

/**
 * 队列项（用于设置播放队列 + 通知栏元数据显示）
 */
data class QueueItem(
    val uri: String,
    val title: String,
    val artist: String,
    val album: String = ""
) {
    fun toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build()
            )
            .build()
    }
}
