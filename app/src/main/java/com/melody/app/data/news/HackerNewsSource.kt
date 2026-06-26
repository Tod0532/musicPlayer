package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
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
    suspend fun fetch(limit: Int = 15): List<NewsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NewsItem>()
        try {
            // 1. 取 top stories（前 100 条 ID）
            val ids = fetchJsonArray("$BASE/topstories.json")?.take(100) ?: return@withContext result

            // 2. 逐条取详情，过滤 AI 相关（并发优化：只取前 100 条，够筛出 AI 内容）
            for (id in ids) {
                if (result.size >= limit) break
                try {
                    val item = fetchItem(id.toLong()) ?: continue
                    // 标题或 URL 含 AI 关键词才保留
                    val checkText = "${item.title} ${item.url}"
                    if (NewsItem.isAiRelated(checkText)) {
                        result.add(item)
                    }
                } catch (_: Exception) { /* 跳过单条失败 */ }
            }
        } catch (_: Exception) { /* 整体失败返回已收集的 */ }
        result
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
     * 生成摘要：标题 + （如果 URL 可达，尝试取正文前几句，否则只用标题）
     * 简化版：只用标题作为摘要基础（避免大量额外网络请求）
     */
    private fun generateSummary(title: String, url: String): String {
        // 截取式摘要：标题本身就是核心信息
        // 完整正文抓取太慢（每条要额外请求），这里用标题
        return title
    }
}
