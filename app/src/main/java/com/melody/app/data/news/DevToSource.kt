package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Dev.to 数据源
 *
 * 抓取 AI 标签的技术文章（开发者视角的 AI 实践）。
 * API: https://dev.to/api/articles?tag=ai
 * 返回 JSON 数组。
 */
object DevToSource {

    /**
     * 抓取 AI 相关文章
     * @param limit 最多返回条数
     */
    suspend fun fetch(limit: Int = 10): List<NewsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NewsItem>()
        try {
            // 抓取多个 AI 相关标签
            val tags = listOf("ai", "machinelearning", "datascience")
            for (tag in tags) {
                if (result.size >= limit) break
                val urlStr = "https://dev.to/api/articles?tag=$tag&per_page=5&top=7"
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 Melody/1.0")
                    connectTimeout = 7000
                    readTimeout = 7000
                }
                if (conn.responseCode != 200) {
                    conn.disconnect()
                    continue
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val array = JSONArray(text)
                for (i in 0 until array.length()) {
                    if (result.size >= limit) break
                    val obj = array.getJSONObject(i)
                    val title = obj.optString("title", "").trim()
                    if (title.isBlank()) continue
                    val description = obj.optString("description", "").trim()
                    val readableUrl = obj.optString("readable_url", "")
                    val url = obj.optString("url", "")
                    val publishedAt = obj.optString("published_at", "")
                    val positiveReactions = obj.optInt("positive_reactions_count", 0)
                    val comments = obj.optInt("comments_count", 0)
                    val readingTime = obj.optInt("reading_time_minutes", 0)
                    val tags = obj.optString("tag_list", "").trim()
                    val author = obj.getJSONObject("user").optString("name", "").ifBlank {
                        obj.getJSONObject("user").optString("username", "")
                    }

                    // 增强摘要：描述 + 阅读时长 + 标签
                    val enhancedSummary = buildString {
                        if (description.isNotBlank()) append(description)
                        if (readingTime > 0) append("。预计阅读${readingTime}分钟")
                        if (positiveReactions > 0) append("，${positiveReactions}人点赞")
                        if (comments > 0) append("，${comments}条评论")
                    }.take(300)

                    result.add(
                        NewsItem(
                            id = "devto_${url.hashCode()}",
                            title = title,
                            summary = enhancedSummary.ifBlank { title },
                            source = "Dev.to",
                            url = url,
                            publishedAt = parseDate(publishedAt),
                            score = positiveReactions,
                            author = author,
                            category = "技术博客"
                        )
                    )
                }
            }
        } catch (_: Exception) { }
        // 质量过滤：只保留点赞数 ≥ 5 的（淘汰低质量内容）
        result.filter { it.score >= 5 }
    }

    private fun parseDate(isoDate: String): Long {
        if (isoDate.isBlank()) return System.currentTimeMillis()
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(isoDate)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
