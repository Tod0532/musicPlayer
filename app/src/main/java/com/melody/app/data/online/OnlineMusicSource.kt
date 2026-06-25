package com.melody.app.data.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 多音源聚合搜索
 *
 * 功能：搜索关键词 → 返回带播放链接的结果列表
 *
 * 音源（全部合法授权）：
 * 1. SoundHelix —— CC BY 4.0 协议音乐，可直接流式播放
 * 2. MusicBrainz —— 开放元数据检索（歌名/歌手/专辑信息），不含音频
 *
 * 返回的 SearchResult 包含 playUrl，可直接交给 Media3 流式播放。
 */
object OnlineMusicSource {

    private const val USER_AGENT = "Melody/1.0"

    // 封面色板
    private val palette = listOf(
        0xFF667EEA to 0xFF764BA2,
        0xFFF472B6 to 0xFFFB923C,
        0xFF34D399 to 0xFF06B6D4,
        0xFFFBBF24 to 0xFFEF4444,
        0xFFA78BFA to 0xFFF472B6,
        0xFF60A5FA to 0xFF34D399
    )

    /**
     * SoundHelix CC 音乐库（可直接播放的 URL）
     */
    private val soundhelixLibrary = listOf(
        Triple("SoundHelix Song 1", "T. Schürger", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
        Triple("SoundHelix Song 2", "T. Schürger", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
        Triple("SoundHelix Song 3", "T. Schürger", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
        Triple("SoundHelix Song 4", "T. Schürger", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
        Triple("SoundHelix Song 5", "T. Schürger", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
        Triple("SoundHelix Song 6", "T. Schürger", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3"),
        Triple("SoundHelix Song 7", "T. Schürger", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3"),
        Triple("SoundHelix Song 8", "T. Schürger", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3")
    )

    /**
     * 【核心】搜索并返回带播放链接的结果
     *
     * @param query 搜索关键词（歌名/歌手）；空字符串返回全部
     * @return 带 playUrl 的搜索结果列表，可直接播放
     */
    suspend fun searchWithPlayUrl(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()

        // 1. 从 SoundHelix CC 库中筛选
        val matched = soundhelixLibrary.filter { (title, artist, _) ->
            query.isBlank() ||
            title.contains(query, ignoreCase = true) ||
            artist.contains(query, ignoreCase = true)
        }

        matched.forEachIndexed { index, (title, artist, url) ->
            val colors = palette[index % palette.size]
            results.add(
                SearchResult(
                    id = url,
                    title = title,
                    artist = artist,
                    album = "SoundHelix",
                    duration = 0L,
                    playUrl = url,
                    source = "SoundHelix",
                    coverColor = colors.first,
                    coverColor2 = colors.second,
                    license = "CC BY 4.0"
                )
            )
        }

        // 2. 如果关键词非空，尝试从 MusicBrainz 补充元数据（演示元数据检索能力）
        // MusicBrainz 只提供元数据，无音频 URL，所以不加入可播放结果
        // 但可用于"搜索建议"或歌词关联（后续功能）
        if (query.isNotBlank()) {
            try {
                val mbCount = queryMusicBrainzCount(query)
                // 元数据命中数记入日志（供调试），不影响可播放结果
                System.err.println("MELODY_SEARCH: MusicBrainz 元数据命中 $mbCount 条（仅元数据，不含音频）")
            } catch (_: Exception) { }
        }

        results
    }

    /**
     * MusicBrainz 元数据检索（开放 API，无需 key）
     * 仅返回元数据，不含音频播放链接
     *
     * @return 匹配的录音数量
     */
    private fun queryMusicBrainzCount(query: String): Int {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val urlStr = "https://musicbrainz.org/ws/2/recording?query=$encoded&fmt=json&limit=1"
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            connectTimeout = 6000
            readTimeout = 6000
        }
        return try {
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(text).optInt("count", 0)
            } else 0
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 获取全部可播放的 CC 音乐（用于"发现"页）
     */
    suspend fun getAllPlayable(): List<SearchResult> = searchWithPlayUrl("")
}
