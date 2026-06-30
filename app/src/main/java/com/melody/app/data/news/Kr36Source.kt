package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 36kr 中文科技新闻源
 *
 * 使用 CustomDns 绕过设备 DNS 问题，直连 36kr API。
 * 内容为原生中文，无需翻译。
 */
object Kr36Source {

    /**
     * 抓取 36kr 快讯（中文科技新闻）
     */
    suspend fun fetch(limit: Int = 10): List<NewsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NewsItem>()
        try {
            val conn = CustomDns.openConnection(
                "https://36kr.com/api/newsflash?per_page=$limit",
                timeoutMs = 10000
            )
            conn.requestMethod = "GET"

            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext result
            }

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(text)
            val items = json.optJSONObject("data")?.optJSONArray("items") ?: return@withContext result

            for (i in 0 until items.length()) {
                if (result.size >= limit) break
                val obj = items.getJSONObject(i)
                val title = obj.optString("title", "").trim()
                if (title.isBlank()) continue

                val description = obj.optString("description", "").trim()
                val publishedAt = obj.optString("published_at", "")
                val newsUrl = obj.optString("news_url", "")
                val id = obj.optInt("id", 0)

                result.add(
                    NewsItem(
                        id = "36kr_$id",
                        title = title,
                        summary = description.ifBlank { title }.take(300),
                        source = "36氪",
                        url = if (newsUrl.isNotBlank()) newsUrl else "https://36kr.com/newsflash/$id",
                        publishedAt = parseDate(publishedAt),
                        score = 0,
                        author = "36氪",
                        category = "科技快讯"
                    )
                )
            }
        } catch (e: Exception) {
            System.err.println("MelodyNews: 36kr 失败: ${e.message}")
        }
        result
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
            format.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
