package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * 量子位 RSS 源（中文 AI 资讯）
 *
 * 直接抓取量子位 RSS feed，原生中文内容。
 * 使用 CustomDns 绕过设备 DNS 问题。
 */
object QbitaiSource {

    suspend fun fetch(limit: Int = 10): List<NewsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NewsItem>()
        try {
            val conn = CustomDns.openConnection(
                "http://www.qbitai.com/feed",  // HTTP 避免 SSL 证书问题
                timeoutMs = 10000
            )
            conn.requestMethod = "GET"

            if (conn.responseCode != 200) {
                System.err.println("MelodyNews: 量子位 HTTP ${conn.responseCode}")
                conn.disconnect()
                return@withContext result
            }

            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            System.err.println("MelodyNews: 量子位 RSS ${xml.length} 字节")

            parseRss(xml, limit)
        } catch (e: Exception) {
            System.err.println("MelodyNews: 量子位 失败: ${e.message}")
            result
        }
    }

    private fun parseRss(xml: String, limit: Int): List<NewsItem> {
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
                            "link" -> if (inItem && link.isBlank()) link = parser.nextText().trim()
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
                                        id = "qbitai_${link.hashCode()}",
                                        title = title,
                                        summary = cleanHtml(description).ifBlank { title }.take(300),
                                        source = NewsItem.SOURCE_QBITAI,
                                        url = link.ifBlank { "https://www.qbitai.com" },
                                        publishedAt = parseDate(pubDate),
                                        score = 0,
                                        author = "量子位",
                                        category = "AI 资讯"
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
            .replace("&#8216;", "'")
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
