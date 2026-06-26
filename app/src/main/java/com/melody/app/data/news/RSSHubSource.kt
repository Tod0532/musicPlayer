package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * RSSHub 数据源（降级源，中文 AI 媒体）
 *
 * 通过 RSSHub 公共节点抓取机器之心、量子位的 RSS
 */
object RSSHubSource {

    // RSSHub 公共节点 + 中文 AI 媒体路由
    private val feeds = listOf(
        "https://rsshub.app/jiqizhixin" to NewsItem.SOURCE_JIQIZHIXIN,
        "https://rsshub.app/qbitai" to NewsItem.SOURCE_QBITAI
    )

    /**
     * 抓取中文 AI 媒体资讯
     * @param limit 每个源最多返回条数
     */
    suspend fun fetch(limit: Int = 8): List<NewsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NewsItem>()
        for ((url, source) in feeds) {
            if (result.size >= limit * feeds.size) break
            try {
                val items = fetchFeed(url, source, limit)
                result.addAll(items)
            } catch (_: Exception) { /* 单个源失败跳过 */ }
        }
        result
    }

    private fun fetchFeed(urlStr: String, source: String, limit: Int): List<NewsItem> {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 Melody/1.0")
            connectTimeout = 3000   // RSSHub 3秒超时，失败静默跳过
            readTimeout = 3000
        }
        return try {
            if (conn.responseCode != 200) return emptyList()
            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            parseRss(xml, source, limit)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 解析 RSS XML（标准格式）
     */
    private fun parseRss(xml: String, source: String, limit: Int): List<NewsItem> {
        val result = mutableListOf<NewsItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xml.reader())

            var title = ""
            var link = ""
            var description = ""
            var pubDate = ""
            var inItem = false

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT && result.size < limit) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name?.lowercase()) {
                            "item" -> {
                                inItem = true
                                title = ""; link = ""; description = ""; pubDate = ""
                            }
                            "title" -> if (inItem) title = parser.nextText().trim()
                            "link" -> if (inItem) link = parser.nextText().trim()
                            "description" -> if (inItem) description = parser.nextText().trim()
                            "pubdate", "published" -> if (inItem) pubDate = parser.nextText().trim()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name?.lowercase() == "item" && inItem) {
                            inItem = false
                            if (title.isNotBlank()) {
                                result.add(
                                    NewsItem(
                                        id = "${source}_${link.hashCode()}",
                                        title = title,
                                        // description 可能含 HTML，简单清理
                                        summary = cleanHtml(description).ifBlank { title }.take(200),
                                        source = source,
                                        url = link,
                                        publishedAt = parseDate(pubDate),
                                        score = 0
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) { /* 解析失败返回已收集的 */ }
        return result
    }

    private fun cleanHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").replace("&amp;", "&").trim()
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        // 尝试解析 RFC 822 日期（RSS 标准）
        return try {
            val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
