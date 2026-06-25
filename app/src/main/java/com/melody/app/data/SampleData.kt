package com.melody.app.data

import com.melody.app.domain.model.CoverColors
import com.melody.app.domain.model.LyricLine
import com.melody.app.domain.model.Song

/**
 * 示例数据（演示用，真实 APP 从 MediaStore 扫描）
 */
object SampleData {

    val songs = listOf(
        Song(
            id = 1,
            title = "晴天",
            artist = "周杰伦",
            album = "叶惠美",
            duration = 269000,
            coverColor = CoverColors.GRADIENT_BLUE,
            coverColor2 = CoverColors.GRADIENT_PURPLE,
            isFavorite = true,
            audioAsset = "song_qingtian.wav"
        ),
        Song(
            id = 2,
            title = "七里香",
            artist = "周杰伦",
            album = "七里香",
            duration = 238000,
            coverColor = CoverColors.GRADIENT_PINK,
            coverColor2 = CoverColors.GRADIENT_ORANGE,
            audioAsset = "song_qilixiang.wav"
        ),
        Song(
            id = 3,
            title = "稻香",
            artist = "周杰伦",
            album = "魔杰座",
            duration = 223000,
            coverColor = CoverColors.GRADIENT_GREEN,
            coverColor2 = CoverColors.GRADIENT_CYAN,
            audioAsset = "song_daoxiang.wav"
        ),
        Song(
            id = 4,
            title = "青花瓷",
            artist = "周杰伦",
            album = "我很忙",
            duration = 238000,
            coverColor = CoverColors.GRADIENT_YELLOW,
            coverColor2 = CoverColors.GRADIENT_RED
        ),
        Song(
            id = 5,
            title = "夜曲",
            artist = "周杰伦",
            album = "十一月的萧邦",
            duration = 225000,
            coverColor = CoverColors.GRADIENT_BLUE,
            coverColor2 = CoverColors.GRADIENT_CYAN
        ),
        Song(
            id = 6,
            title = "简单爱",
            artist = "周杰伦",
            album = "范特西",
            duration = 268000,
            coverColor = CoverColors.GRADIENT_PURPLE,
            coverColor2 = CoverColors.GRADIENT_PINK
        ),
        Song(
            id = 7,
            title = "告白气球",
            artist = "周杰伦",
            album = "周杰伦的床边故事",
            duration = 215000,
            coverColor = CoverColors.GRADIENT_PINK,
            coverColor2 = CoverColors.GRADIENT_RED
        ),
        Song(
            id = 8,
            title = "等你下课",
            artist = "周杰伦",
            album = "单曲",
            duration = 269000,
            coverColor = CoverColors.GRADIENT_ORANGE,
            coverColor2 = CoverColors.GRADIENT_YELLOW
        )
    )

    val sampleLyrics = listOf(
        LyricLine(0, "故事的小黄花"),
        LyricLine(4000, "从出生那年就飘着"),
        LyricLine(9000, "少年的冲动"),
        LyricLine(14000, "熟悉的暗涌"),
        LyricLine(19000, "童年的荡秋千"),
        LyricLine(24000, "随记忆一直晃到现在"),
        LyricLine(30000, "Re So So Si Do Si La"),
        LyricLine(35000, "So La Si Si Si Si La Si La So"),
        LyricLine(40000, "吹着前奏望着天空"),
        LyricLine(45000, "我想起花瓣试着掉落"),
        LyricLine(50000, "为你翘课的那一天"),
        LyricLine(55000, "花落的那一天"),
        LyricLine(60000, "教室的那一间"),
        LyricLine(65000, "我怎么看不见"),
        LyricLine(70000, "消失的下雨天"),
        LyricLine(75000, "我好想再淋一遍")
    )
}
