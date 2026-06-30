package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL

/**
 * 顶级科技媒体 AI 资讯源
 *
 * 抓取全球顶级 AI 媒体的 RSS：
 * - TechCrunch AI（行业新闻）
 * - The Verge AI（深度报道）
 * - MIT Technology Review（前沿视角）
 *
 * 这些源内容质量高，覆盖重磅 AI 行业动态。
 */
object TechMediaSource {

    private val feeds = listOf(
        FeedConfig(
            url = "https://techcrunch.com/category/artificial-intelligence/feed/",
            source = "TechCrunch",
            category = "行业新闻",
            color = 0xFF00D100
        ),
        FeedConfig(
            url = "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml",
            source = "The Verge",
            category = "深度报道",
            color = 0xFFE847C7
        ),
        FeedConfig(
            url = "https://www.wired.com/feed/tag/ai/latest/rss",
            source = "WIRED",
            category = "科技洞察",
            color = 0xFF000000.toLong().or(0xFF888888)  // WIRED 灰
        ),
        FeedConfig(
            url = "https://venturebeat.com/category/ai/feed/",
            source = "VentureBeat",
            category = "AI商业",
            color = 0xFFFF6B35  // VB 橙
        )
    )

    /**
     * 抓取顶级媒体的 AI 资讯
     * @param limitPerFeed 每个源最多返回条数
     */
    suspend fun fetch(limitPerFeed: Int = 5): List<NewsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NewsItem>()
        for (feed in feeds) {
            try {
                val items = fetchFeed(feed, limitPerFeed)
                result.addAll(items)
            } catch (_: Exception) { /* 单源失败跳过 */ }
        }
        // 过滤：只保留 AI 相关的（MIT feed 是全站，需筛选）
        result.filter { item ->
            NewsItem.isAiRelated("${item.title} ${item.summary}") ||
            item.source == "TechCrunch"  // TechCrunch AI 频道本身就是 AI
        }
    }

    private fun fetchFeed(feed: FeedConfig, limit: Int): List<NewsItem> {
        val conn = (URL(feed.url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Melody/1.0")
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
        }
        return try {
            if (conn.responseCode != 200) return emptyList()
            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            parseRssOrAtom(xml, feed, limit)
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 解析 RSS（item）或 Atom（entry）格式
     */
    private fun parseRssOrAtom(xml: String, feed: FeedConfig, limit: Int): List<NewsItem> {
        val result = mutableListOf<NewsItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xml.reader())

            var title = ""
            var link = ""
            var description = ""
            var pubDate = ""
            var author = ""
            var inItem = false
            val itemTag = if (xml.contains("<entry>")) "entry" else "item"

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT && result.size < limit) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name?.lowercase()) {
                            itemTag -> {
                                inItem = true
                                title = ""; link = ""; description = ""; pubDate = ""; author = ""
                            }
                            "title" -> if (inItem) {
                                title = decodeHtml(parser.nextText().trim())
                            }
                            "link" -> if (inItem && link.isBlank()) {
                                // Atom 的 link 是空标签，href 在属性里
                                val href = parser.getAttributeValue(null, "href")
                                if (href != null) link = href.trim()
                            }
                            "description", "summary", "content" -> if (inItem) {
                                description = decodeHtml(parser.nextText().trim())
                            }
                            "pubdate", "published", "updated" -> if (inItem) {
                                pubDate = parser.nextText().trim()
                            }
                            "creator", "author" -> if (inItem) {
                                author = decodeHtml(parser.nextText().trim())
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        // link 标签结束时（RSS 格式 link 是文本），如果还是空，尝试读文本
                        if (parser.name?.lowercase() == "link" && inItem && link.isBlank()) {
                            // RSS 的 link 在标签文本里，但 nextText 已在 START_TAG 处理
                            // Atom 的 href 在 START_TAG 已处理
                        }
                        if (parser.name?.lowercase() == itemTag && inItem) {
                            inItem = false
                            if (title.isNotBlank()) {
                                result.add(
                                    NewsItem(
                                        id = "${feed.source}_${link.hashCode()}",
                                        title = title,
                                        summary = cleanHtml(description).ifBlank { title }.take(300),
                                        source = feed.source,
                                        url = link.ifBlank { feed.url },
                                        publishedAt = parseDate(pubDate),
                                        score = 0,
                                        author = author,
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

    private fun decodeHtml(text: String): String {
        return text
            .replace("&#8217;", "'")
            .replace("&#8216;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#8211;", "-")
            .replace("&#8212;", "—")
            .replace("&#038;", "&")
            .replace("&#8230;", "...")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .trim()
    }

    private fun cleanHtml(html: String): String {
        return decodeHtml(html).replace(Regex("<[^>]*>"), "").trim()
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        // 尝试多种日期格式
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss z" to java.util.Locale.US,  // RFC 822 (RSS)
            "yyyy-MM-dd'T'HH:mm:ss'Z'" to java.util.Locale.US,     // ISO 8601 (Atom)
            "yyyy-MM-dd'T'HH:mm:ssXXX" to java.util.Locale.US      // ISO 8601 帽时区
        )
        for ((pattern, locale) in formats) {
            try {
                val format = java.text.SimpleDateFormat(pattern, locale)
                if (pattern.endsWith("'Z'")) format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                format.parse(dateStr)?.let { return it.time }
            } catch (_: Exception) { }
        }
        return System.currentTimeMillis()
    }

    private data class FeedConfig(
        val url: String,
        val source: String,
        val category: String,
        val color: Long
    )
}
