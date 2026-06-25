package com.melody.app.data.online

/**
 * 在线搜索结果（带播放链接）
 *
 * 一个搜索结果 = 元数据（歌名/歌手/专辑）+ 可播放的音频 URL
 */
data class SearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,           // 毫秒，0 表示未知
    val playUrl: String,          // 可直接播放的音频 URL（Media3 可流式播放）
    val source: String,           // 音源标识（"SoundHelix" / "Jamendo" / ...）
    val coverColor: Long,         // 封面占位色
    val coverColor2: Long,
    val license: String = "CC"    // 授权协议
) {
    /**
     * 转换为可播放的 Song（用于接入播放器）
     */
    fun toSong(): com.melody.app.domain.model.Song = com.melody.app.domain.model.Song(
        id = id.hashCode().toLong(),
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        coverColor = coverColor,
        coverColor2 = coverColor2,
        mediaUri = playUrl   // 播放链接直接作为 mediaUri
    )
}
