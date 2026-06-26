package com.melody.app.data.news

import com.melody.app.media.NewsTranslator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 资讯聚合仓库
 *
 * 并发抓取三个数据源 → 翻译英文内容为中文 → 合并去重排序
 */
object NewsRepository {

    /**
     * 抓取全部 AI 资讯（三源并发 + 英文翻译）
     */
    suspend fun fetchAllNews(): List<NewsItem> = coroutineScope {
        // 1. 并发抓取三个源
        val hnDeferred = async { HackerNewsSource.fetch() }
        val ghDeferred = async { GitHubTrendingSource.fetch() }
        val rssDeferred = async { RSSHubSource.fetch() }

        val hn = hnDeferred.await()
        val gh = ghDeferred.await()
        val rss = rssDeferred.await()

        // 2. 合并 + 去重
        val merged = (hn + gh + rss)
            .distinctBy { normalizeTitle(it.title) }

        // 3. 翻译英文内容为中文（HackerNews/GitHub 通常是英文）
        val translated = translateItems(merged)

        // 4. 按时间倒序排序
        translated.sortedByDescending { it.publishedAt }
    }

    /**
     * 翻译新闻列表中的英文内容
     * 只翻译检测为英文的标题和摘要，中文内容保持原样
     */
    private suspend fun translateItems(items: List<NewsItem>): List<NewsItem> {
        // 确保翻译模型就绪（首次会下载，约30MB）
        val modelReady = NewsTranslator.ensureReady()
        if (!modelReady) return items  // 模型不可用，返回原文

        return items.map { item ->
            val titleEn = item.title
            val summaryEn = item.summary

            // 翻译标题（如果是英文）
            val titleCn = if (NewsTranslator.isLikelyEnglish(titleEn)) {
                NewsTranslator.translate(titleEn)
            } else titleEn

            // 翻译摘要（如果是英文）
            val summaryCn = if (summaryEn != titleEn && NewsTranslator.isLikelyEnglish(summaryEn)) {
                NewsTranslator.translate(summaryEn)
            } else summaryEn

            // 保留原文标题在摘要前（方便用户对照）
            val finalSummary = if (titleEn != titleCn && summaryEn.isNotBlank()) {
                "$summaryCn"
            } else summaryCn

            item.copy(title = titleCn, summary = finalSummary)
        }
    }

    /**
     * 标题归一化（用于去重）：转小写 + 去空格标点
     */
    private fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]"), "")
            .take(50)
    }
}
