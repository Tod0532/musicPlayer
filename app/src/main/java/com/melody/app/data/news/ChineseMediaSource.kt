package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * 中文科技媒体 RSS 源（多源聚合）
 *
 * 通过 HTTP + CustomDns IP 直连，绕过设备 DNS 问题。
 * 支持多个中文科技媒体的 RSS feed。
 */
object ChineseMediaSource {

    private data class Feed(val host: String, val path: String, val source: String, val category: String)

    private val feeds = listOf(
        Feed("www.ithome.com", "/rss/", "IT之家", "科技资讯"),
        Feed("www.ifanr.com", "/feed", "爱范儿", "科技资讯"),
        Feed("sspai.com", "/feed", "少数派", "效率工具"),
        Feed("www.leiphone.com", "/feed", "雷锋网", "AI资讯")
    )

    /**
     * 抓取所有中文媒体 RSS
     * @param limitPerFeed 每个源最多返回条数
     */
    suspend fun fetch(limitPerFeed: Int = 5): List<NewsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NewsItem>()
        for (feed in feeds) {
            if (result.size >= 20) break
            try {
                val items = fetchFeed(feed, limitPerFeed)
                result.addAll(items)
            } catch (e: Exception) {
                System.err.println("MelodyNews: ${feed.source} 失败: ${e.message}")
            }
        }
        // 过滤 AI 相关内容
        result.filter { item ->
            // 少数派/爱范儿内容宽泛，需 AI 关键词过滤
            // IT之家/雷锋网本身就偏科技，保留
            item.source == "IT之家" || item.source == "雷锋网" ||
            NewsItem.isAiRelated("${item.title} ${item.summary}")
        }
    }

    private fun fetchFeed(feed: Feed, limit: Int): List<NewsItem> {
        val conn = CustomDns.openConnection(
            "http://${feed.host}${feed.path}",
            timeoutMs = 8000
        )
        conn.requestMethod = "GET"

        if (conn.responseCode != 200) {
            conn.disconnect()
            return emptyList()
        }

        val xml = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        return parseRss(xml, feed, limit)
    }

    private fun parseRss(xml: String, feed: Feed, limit: Int): List<NewsItem> {
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
                            "link" -> if (inItem && link.isBlank()) {
                                link = try { parser.nextText().trim() } catch (_: Exception) { "" }
                            }
                            "description" -> if (inItem) description = parser.nextText().trim()
                            "pubdate" -> if (inItem) pubDate = parser.nextText().trim()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name?.lowercase() == "item" && inItem) {
                            inItem = false
                            if (title.isNotBlank()) {
                                result.add(
                                    NewsItem(
                                        id = "${feed.source}_${link.hashCode()}",
                                        title = title,
                                        summary = cleanHtml(description).ifBlank { title }.take(300),
                                        source = feed.source,
                                        url = link.ifBlank { "http://${feed.host}" },
                                        publishedAt = parseDate(pubDate),
                                        score = 0,
                                        author = feed.source,
                                        category = feed.category
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) { }
        return result
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&#8217;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#8230;", "...")
            .trim()
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US)
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
