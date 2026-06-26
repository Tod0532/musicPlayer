package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * HackerNews 数据源（官方 API，最可靠）
 *
 * 抓取 top stories，按 AI 关键词过滤
 */
object HackerNewsSource {

    private const val BASE = "https://hacker-news.firebaseio.com/v0"

    /**
     * 抓取 AI 相关的热门新闻
     * @param limit 最多返回条数
     */
    suspend fun fetch(limit: Int = 15): List<NewsItem> = kotlinx.coroutines.coroutineScope {
        val ids = withContext(Dispatchers.IO) {
            fetchJsonArray("$BASE/topstories.json")?.take(50)
        } ?: return@coroutineScope emptyList()

        // 并发取详情（coroutineScope 内可直接 async）
        val deferredItems = ids.map { id ->
            async(Dispatchers.IO) {
                try { fetchItem(id.toLong()) } catch (_: Exception) { null }
            }
        }
        val items = deferredItems.awaitAll().filterNotNull()

        // 过滤 AI 相关，按热度排序
        items.filter { NewsItem.isAiRelated("${it.title} ${it.url}") }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun fetchJsonArray(urlStr: String): List<String>? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
        }
        return try {
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                // 简单解析 JSON 数组 [id1, id2, ...]
                text.trim().trim('[', ']').split(",").map { it.trim() }
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchItem(id: Long): NewsItem? {
        val conn = (URL("$BASE/item/$id.json").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }
        return try {
            if (conn.responseCode != 200) return null
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val title = json.optString("title", "")
            if (title.isBlank()) return null
            val url = json.optString("url", "")
            val score = json.optInt("score", 0)
            val time = json.optLong("time", 0) * 1000  // HN 时间是秒，转毫秒

            NewsItem(
                id = "hn_$id",
                title = title,
                summary = generateSummary(title, url),
                source = NewsItem.SOURCE_HACKERNEWS,
                url = if (url.isNotBlank()) url else "https://news.ycombinator.com/item?id=$id",
                publishedAt = time,
                score = score
            )
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 生成摘要：抓取原文的 meta description 或正文首段
     * 失败则用标题
     */
    private fun generateSummary(title: String, url: String): String {
        if (url.isBlank()) return title
        return try {
            val doc = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android 14) Melody/1.0")
                .timeout(4000)
                .get()
            // 优先 meta description
            val metaDesc = doc.select("meta[name=description]").attr("content").trim()
            if (metaDesc.length > 20) return metaDesc.take(300)
            // 其次 og:description
            val ogDesc = doc.select("meta[property=og:description]").attr("content").trim()
            if (ogDesc.length > 20) return ogDesc.take(300)
            // 最后正文首段
            val firstP = doc.select("p").first()?.text()?.trim()
            if (!firstP.isNullOrBlank() && firstP.length > 20) firstP.take(300) else title
        } catch (_: Exception) {
            title
        }
    }
}
