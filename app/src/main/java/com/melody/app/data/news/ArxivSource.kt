package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * ArXiv 论文数据源
 *
 * 抓取 cs.AI（人工智能）分类的最新论文。
 * ArXiv API: http://export.arxiv.org/api/query
 * 返回 Atom XML 格式。
 */
object ArxivSource {

    private const val BASE = "http://export.arxiv.org/api/query"

    /**
     * 抓取最新 AI 论文
     * @param limit 最多返回条数
     */
    suspend fun fetch(limit: Int = 10): List<NewsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NewsItem>()
        try {
            // 查询 cs.AI 分类，按提交时间倒序
            val urlStr = "$BASE?search_query=cat:cs.AI&sortBy=submittedDate&sortOrder=descending&max_results=$limit"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 Melody/1.0")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode != 200) return@withContext result
            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            parseAtom(xml)
        } catch (_: Exception) {
            result
        }
    }

    /**
     * 解析 ArXiv Atom XML
     */
    private fun parseAtom(xml: String): List<NewsItem> {
        val result = mutableListOf<NewsItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xml.reader())

            var title = ""
            var summary = ""
            var link = ""
            var published = ""
            var inEntry = false
            // Atom 用 namespace，name 不带前缀
            var ns = ""

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name?.lowercase() ?: ""
                        when (name) {
                            "entry" -> {
                                inEntry = true
                                title = ""; summary = ""; link = ""; published = ""
                            }
                            "title" -> if (inEntry) title = parser.nextText().trim().replace("\n", " ")
                            "summary" -> if (inEntry) summary = parser.nextText().trim().replace("\n", " ")
                            "id" -> if (inEntry && link.isBlank()) link = parser.nextText().trim()
                            "published" -> if (inEntry) published = parser.nextText().trim()
                            "link" -> {
                                if (inEntry && link.isBlank()) {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (href != null) link = href.trim()
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name?.lowercase() == "entry" && inEntry) {
                            inEntry = false
                            if (title.isNotBlank()) {
                                result.add(
                                    NewsItem(
                                        id = "arxiv_${link.hashCode()}",
                                        title = "📄 $title",
                                        // 论文摘要可能很长，截取前 300 字符
                                        summary = summary.take(300),
                                        source = "ArXiv",
                                        url = link,
                                        publishedAt = parseDate(published),
                                        score = 0
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

    private fun parseDate(isoDate: String): Long {
        if (isoDate.isBlank()) return System.currentTimeMillis()
        return try {
            // ISO 8601: 2026-06-29T12:00:00Z
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(isoDate)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
