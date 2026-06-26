package com.melody.app.media

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.melody.app.data.news.NewsItem
import java.util.Locale

/**
 * 新闻播报控制器（基于 Android TextToSpeech）
 *
 * 把 NewsItem 的文字朗读出来，支持连续播报（一条接一条）。
 * 接口设计与音乐播放器对齐：play/pause/next/previous。
 */
class NewsPlayerController(context: Context) {

    private var tts: TextToSpeech? = null
    private var queue: List<NewsItem> = emptyList()
    private var currentIndex = 0
    private var isReady = false
    private var isPlayingFlag = false
    private var pendingStart: List<NewsItem>? = null

    // 回调
    var onStateChanged: ((isPlaying: Boolean, index: Int, total: Int) -> Unit)? = null
    var onCompleted: (() -> Unit)? = null
    var onTtsNotAvailable: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINESE)
                // CHINESE 不一定可用，尝试 SIMPLIFIED_CHINESE 或回退
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                }
                isReady = true
                // 引擎就绪后执行暂存的播放请求
                pendingStart?.let { items ->
                    pendingStart = null
                    startPlayback(items, 0)
                }
            } else {
                onTtsNotAvailable?.invoke()
            }
        }

        // 监听朗读进度
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val idx = utteranceId?.toIntOrNull() ?: return
                currentIndex = idx
                isPlayingFlag = true
                onStateChanged?.invoke(true, currentIndex, queue.size)
            }

            override fun onDone(utteranceId: String?) {
                // 当前条朗读完成，自动播下一条
                val next = currentIndex + 1
                if (next < queue.size) {
                    speakItem(next)
                } else {
                    // 全部播完
                    isPlayingFlag = false
                    onStateChanged?.invoke(false, currentIndex, queue.size)
                    onCompleted?.invoke()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onError2(utteranceId)
            }

            private fun onError2(utteranceId: String?) {
                isPlayingFlag = false
                onStateChanged?.invoke(false, currentIndex, queue.size)
            }
        })
    }

    /**
     * 开始播报
     * @param items 新闻列表
     * @param startIndex 从第几条开始
     */
    fun startPlayback(items: List<NewsItem>, startIndex: Int) {
        if (items.isEmpty()) return
        queue = items
        currentIndex = startIndex.coerceIn(0, items.lastIndex)
        if (isReady) {
            speakItem(currentIndex)
        } else {
            // TTS 引擎未就绪，暂存
            pendingStart = items
        }
    }

    /**
     * 恢复播报（从当前条重新开始）
     */
    fun play() {
        if (queue.isEmpty()) return
        speakItem(currentIndex)
    }

    /**
     * 暂停（TTS 无真正暂停，只能 stop，恢复时重读当前条）
     */
    fun pause() {
        tts?.stop()
        isPlayingFlag = false
        onStateChanged?.invoke(false, currentIndex, queue.size)
    }

    /**
     * 下一条
     */
    fun next() {
        if (queue.isEmpty()) return
        val nextIdx = currentIndex + 1
        if (nextIdx < queue.size) {
            tts?.stop()
            speakItem(nextIdx)
        }
    }

    /**
     * 上一条
     */
    fun previous() {
        if (queue.isEmpty()) return
        val prevIdx = currentIndex - 1
        if (prevIdx >= 0) {
            tts?.stop()
            speakItem(prevIdx)
        }
    }

    fun stop() {
        tts?.stop()
        isPlayingFlag = false
        queue = emptyList()
        currentIndex = 0
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    val currentTitle: String
        get() = queue.getOrNull(currentIndex)?.title ?: ""

    val currentIndexValue: Int get() = currentIndex
    val total: Int get() = queue.size
    val isPlaying: Boolean get() = isPlayingFlag

    /**
     * 朗读指定条目
     */
    private fun speakItem(index: Int) {
        val item = queue.getOrNull(index) ?: return
        currentIndex = index
        val text = formatSpeech(item, index, queue.size)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, index.toString())
        // onStateChanged 在 onStart 回调里触发
    }

    /**
     * 格式化朗读文本
     */
    private fun formatSpeech(item: NewsItem, index: Int, total: Int): String {
        return "第${index + 1}条，共$total 条。来自${item.source}。${item.title}。${item.summary}。"
    }
}
