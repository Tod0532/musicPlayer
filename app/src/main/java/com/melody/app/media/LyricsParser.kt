package com.melody.app.media

import android.content.Context
import com.melody.app.domain.model.LyricLine

/**
 * LRC 歌词解析器
 *
 * 解析标准 LRC 格式：
 * [mm:ss.xx]歌词文本
 *
 * 支持同一行多个时间戳（如 [00:01.00][00:15.00]重复歌词）
 */
object LyricsParser {

    private val timeRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})]""")

    /**
     * 从 assets 读取并解析 LRC 文件
     * @param context 上下文
     * @param assetFileName assets 内的 .lrc 文件名
     * @return 解析后的歌词行列表（按时间排序），失败返回空列表
     */
    fun parseFromAssets(context: Context, assetFileName: String): List<LyricLine> {
        return try {
            val text = context.assets.open(assetFileName).bufferedReader().use { it.readText() }
            parse(text)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 解析 LRC 文本
     */
    fun parse(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()

        lrcText.lines().forEach { line ->
            // 跳过元数据行（如 [ti:歌名]、[ar:歌手]）
            if (line.startsWith("[ti:") || line.startsWith("[ar:") ||
                line.startsWith("[al:") || line.startsWith("[by:") ||
                line.startsWith("[offset:")) return@forEach

            // 找到所有时间戳
            val matches = timeRegex.findAll(line)
            val timestamps = matches.toList()
            if (timestamps.isEmpty()) return@forEach

            // 提取时间戳后的歌词文本
            val lastMatch = timestamps.last()
            val lyricText = line.substring(lastMatch.range.last + 1).trim()
            if (lyticTextIsEmpty(lyricText)) return@forEach

            // 每个时间戳生成一行歌词（处理多时间戳行）
            timestamps.forEach { match ->
                val (min, sec, ms) = match.destructured
                val time = min.toLong() * 60_000 + sec.toLong() * 1000 +
                    (ms.padEnd(3, '0').take(3)).toLong()
                lines.add(LyricLine(time = time, text = lyricText))
            }
        }

        return lines.sortedBy { it.time }
    }

    private fun lyticTextIsEmpty(text: String): Boolean {
        return text.isBlank()
    }

    /**
     * 根据当前播放位置找到对应的歌词索引
     * @param lyrics 歌词列表（已按时间排序）
     * @param currentPosition 当前播放位置（毫秒）
     * @return 当前行索引，-1 表示无匹配
     */
    fun findActiveIndex(lyrics: List<LyricLine>, currentPosition: Long): Int {
        if (lyrics.isEmpty()) return -1
        var activeIndex = 0
        for (i in lyrics.indices) {
            if (lyrics[i].time <= currentPosition) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }
}
