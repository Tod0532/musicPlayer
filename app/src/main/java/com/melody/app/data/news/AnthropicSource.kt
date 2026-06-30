package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

/**
 * Anthropic 官方新闻源
 *
 * 抓取 Anthropic 官方网站的新闻页面（Claude 相关动态）。
 * 国内可达（HTTP 200）。
 */
object AnthropicSource {

    /**
     * 抓取 Anthropic 最新新闻
     * @param limit 最多返回条数
     */
    suspend fun fetch(limit: Int = 8): List<NewsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NewsItem>()
        try {
            val doc = Jsoup.connect("https://www.anthropic.com/news")
                .userAgent("Mozilla/5.0 (Linux; Android 14) Melody/1.0")
                .timeout(10000)
                .get()

            // Anthropic 新闻页面的文章卡片
            val articles = doc.select("article, [class*=news], [class*=post], a[href*=/news/]")
            val now = System.currentTimeMillis()

            for (article in articles) {
                if (result.size >= limit) break
                try {
                    val titleEl = article.select("h2, h3, h4, [class*=title]").first()
                    val title = titleEl?.text()?.trim() ?: continue
                    if (title.length < 5) continue

                    val link = article.select("a[href]").first()?.absUrl("href")?.trim()
                        ?: article.absUrl("href").trim()
                    if (link.isBlank() || !link.contains("anthropic")) continue

                    val desc = article.select("p, [class*=desc], [class*=summary]").first()?.text()?.trim() ?: ""

                    result.add(
                        NewsItem(
                            id = "anthropic_${link.hashCode()}",
                            title = title,
                            summary = desc.ifBlank { title }.take(300),
                            source = "Anthropic",
                            url = link,
                            publishedAt = now,
                            score = 0,
                            author = "Anthropic",
                            category = "官方动态"
                        )
                    )
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
        result
    }
}
