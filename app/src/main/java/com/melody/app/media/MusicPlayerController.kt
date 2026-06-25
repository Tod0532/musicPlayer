package com.melody.app.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.exoplayer.ExoPlayer

/**
 * Media3 播放控制器
 * 封装 ExoPlayer，提供真实音频播放能力
 */
class MusicPlayerController(private val context: Context) {

    private var player: ExoPlayer? = null

    // 回调：播放状态/进度变化时通知 ViewModel
    var onPlaybackStateChanged: ((isPlaying: Boolean, position: Long, duration: Long) -> Unit)? = null
    var onPlaybackCompleted: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val p = player ?: return
            onPlaybackStateChanged?.invoke(isPlaying, p.currentPosition, p.duration.coerceAtLeast(0))
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                onPlaybackCompleted?.invoke()
            } else if (playbackState == Player.STATE_READY) {
                val p = player ?: return
                onPlaybackStateChanged?.invoke(p.isPlaying, p.currentPosition, p.duration.coerceAtLeast(0))
            }
        }
    }

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (player != null) return
        player = ExoPlayer.Builder(context).apply {
            setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
        }.build().also {
            it.addListener(playerListener)
        }
    }

    /**
     * 播放指定歌曲（assets 内文件）
     */
    fun play(assetFileName: String) {
        val p = player ?: return
        val uri = "asset:///$assetFileName"
        val mediaItem = MediaItem.fromUri(Uri.parse(uri))
        p.setMediaItem(mediaItem)
        p.prepare()
        p.playWhenReady = true
    }

    /**
     * 播放 content:// URI（MediaStore 扫描到的真实歌曲）
     */
    fun playUri(uriString: String) {
        val p = player ?: return
        val mediaItem = MediaItem.fromUri(Uri.parse(uriString))
        p.setMediaItem(mediaItem)
        p.prepare()
        p.playWhenReady = true
    }

    fun resume() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun stop() {
        player?.stop()
    }

    fun getCurrentPosition(): Long = player?.currentPosition ?: 0

    fun getDuration(): Long = player?.duration?.coerceAtLeast(0) ?: 0

    fun isPlaying(): Boolean = player?.isPlaying ?: false

    fun release() {
        player?.release()
        player = null
    }
}
