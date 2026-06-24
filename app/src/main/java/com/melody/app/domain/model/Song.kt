package com.melody.app.domain.model

/**
 * 歌曲领域模型（纯 Kotlin，无 Android 依赖）
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,        // 毫秒
    val coverColor: Long = CoverColors.GRADIENT_BLUE,  // 封面占位渐变起始色
    val coverColor2: Long = CoverColors.GRADIENT_PURPLE,
    val isFavorite: Boolean = false
)

/**
 * 歌词行
 */
data class LyricLine(
    val time: Long,     // 该行开始时间（毫秒）
    val text: String
)

/**
 * 播放状态
 */
enum class PlayState { PLAYING, PAUSED, STOPPED }

/**
 * 播放模式
 */
enum class PlayMode { SEQUENCE, SHUFFLE, REPEAT_ONE }

/**
 * 封面色板（占位用，真实 APP 用 Palette 从封面提取）
 */
object CoverColors {
    val GRADIENT_BLUE = 0xFF667EEA
    val GRADIENT_PURPLE = 0xFF764BA2
    val GRADIENT_PINK = 0xFFF472B6
    val GRADIENT_ORANGE = 0xFFFB923C
    val GRADIENT_GREEN = 0xFF34D399
    val GRADIENT_CYAN = 0xFF06B6D4
    val GRADIENT_YELLOW = 0xFFFBBF24
    val GRADIENT_RED = 0xFFEF4444
}
