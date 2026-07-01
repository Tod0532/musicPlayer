package com.melody.app.data.news

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * 静态新闻源（从 assets 加载预抓取的中文 AI 资讯）
 *
 * 内容在构建时预抓取（电脑端可访问量子位等源），
 * 打包进 APK，设备端直接读取，无网络依赖。
 */
object StaticNewsSource {

    suspend fun fetch(context: Context? = null): List<NewsItem> = withContext(Dispatchers.IO) {
        if (context == null) return@withContext emptyList()
        try {
            val json = context.assets.open("static_news.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val result = mutableListOf<NewsItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    NewsItem(
                        id = obj.optString("id", "static_$i"),
                        title = obj.optString("title", ""),
                        summary = obj.optString("summary", ""),
                        source = obj.optString("source", "AI资讯"),
                        url = obj.optString("url", ""),
                        publishedAt = obj.optLong("publishedAt", 0L),
                        score = obj.optInt("score", 0),
                        author = obj.optString("author", ""),
                        category = obj.optString("category", "AI资讯")
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }
}
