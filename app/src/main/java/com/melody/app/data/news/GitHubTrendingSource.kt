package com.melody.app.data.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * GitHub Trending 数据源
 *
 * 抓取 trending 页面，用 Jsoup 解析，按 AI 关键词筛选
 */
object GitHubTrendingSource {

    /**
     * 抓取今日热门 AI 仓库
     * @param limit 最多返回条数
     */
    suspend fun fetch(limit: Int = 10): List<NewsItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NewsItem>()
        try {
            val doc: Document = Jsoup.connect("https://github.com/trending?since=daily")
                .userAgent("Mozilla/5.0 (Linux; Android 14) Melody/1.0")
                .timeout(8000)
                .get()

            // trending 页面每个仓库在 article.Box-row 元素里
            val articles = doc.select("article.Box-row")
            val now = System.currentTimeMillis()

            for (article in articles) {
                if (result.size >= limit) break
                try {
                    val repoLink = article.select("h2 a").first() ?: continue
                    val repoPath = repoLink.attr("href").trim().removePrefix("/")  // 如 "ollama/ollama"
                    if (repoPath.isBlank()) continue

                    val description = article.select("p").text().trim()
                    val language = article.select("[itemprop=programmingLanguage]").text().trim()
                    val starsText = article.select("a[href*=stargazers]").text().trim()
                    val starsTodayText = article.select("span.d-inline-block").last()?.text()?.trim() ?: ""

                    // AI 关键词过滤：描述或仓库名含 AI 关键词
                    val checkText = "$repoPath $description $language"
                    if (!NewsItem.isAiRelated(checkText) && language != "Python") continue

                    val title = "【新项目】$repoPath"
                    val desc = if (description.isBlank()) "热门 AI 项目" else description
                    val url = "https://github.com/$repoPath"
                    val stars = parseStars(starsText)
                    val todayStars = parseTodayStars(starsTodayText)

                    // 增强摘要：描述 + 语言 + 今日 stars
                    val enhancedSummary = buildString {
                        append(desc)
                        if (language.isNotBlank()) append("。语言：$language")
                        if (stars > 0) append("，$stars stars")
                        if (todayStars > 0) append("，今日新增 $todayStars stars")
                    }.take(300)

                    result.add(
                        NewsItem(
                            id = "gh_$repoPath",
                            title = title,
                            summary = enhancedSummary,
                            source = NewsItem.SOURCE_GITHUB,
                            url = url,
                            publishedAt = now,
                            score = stars + todayStars
                        )
                    )
                } catch (_: Exception) { /* 跳过单条 */ }
            }
        } catch (_: Exception) { /* 整体失败返回空 */ }
        result
    }

    private fun parseStars(text: String): Int {
        return text.replace(",", "").replace("k", "000").toIntOrNull() ?: 0
    }

    /**
     * 解析"今日新增 N stars today"
     */
    private fun parseTodayStars(text: String): Int {
        // 格式如 "1,234 stars today"
        val match = Regex("(\\d[\\d,]*)\\s*stars\\s*today").find(text)
        return match?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
    }
}
