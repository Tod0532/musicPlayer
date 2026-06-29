package com.melody.app.data.news

import com.melody.app.media.NewsTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * 资讯聚合仓库
 *
 * 并发抓取三个数据源 → 翻译英文内容为中文 → 合并去重排序
 */
object NewsRepository {

    /**
     * 抓取全部 AI 资讯（三源并发 + 英文翻译 + 关键词过滤）
     * @param context 用于关键词订阅（可为 null 表示不过滤）
     */
    suspend fun fetchAllNews(context: android.content.Context? = null): List<NewsItem> = coroutineScope {
        // 1. 并发抓取四个源
        val hnDeferred = async {
            try { HackerNewsSource.fetch() } catch (_: Exception) { emptyList() }
        }
        val ghDeferred = async {
            try { GitHubTrendingSource.fetch() } catch (_: Exception) { emptyList() }
        }
        val rssDeferred = async {
            try { RSSHubSource.fetch() } catch (_: Exception) { emptyList() }
        }
        val arxivDeferred = async {
            try { ArxivSource.fetch() } catch (_: Exception) { emptyList() }
        }

        val hn = hnDeferred.await()
        val gh = ghDeferred.await()
        val rss = rssDeferred.await()
        val arxiv = arxivDeferred.await()

        System.err.println("MelodyNews: 合计 HN=${hn.size} GH=${gh.size} RSS=${rss.size} ArXiv=${arxiv.size}")

        // 2. 合并 + 去重
        val merged = (hn + gh + rss + arxiv)
            .distinctBy { normalizeTitle(it.title) }

        // 3. 翻译英文内容为中文
        val translated = translateItems(merged)

        // 4. 按来源分类排序（同类连续，播报时按板块过渡）
        val sourceOrder = listOf(
            NewsItem.SOURCE_GITHUB,
            NewsItem.SOURCE_HACKERNEWS,
            NewsItem.SOURCE_ARXIV,
            NewsItem.SOURCE_JIQIZHIXIN,
            NewsItem.SOURCE_QBITAI
        )
        val sorted = translated.sortedWith(
            compareBy<NewsItem> { sourceOrder.indexOf(it.source).let { i -> if (i < 0) 99 else i } }
                .thenByDescending { it.publishedAt }
        )

        // 5. 关键词过滤（用户订阅了关键词则只保留匹配的）
        val keywordSubscription = context?.let { KeywordSubscription.getInstance(it) }
        val filtered = if (keywordSubscription != null) {
            sorted.filter { keywordSubscription.matches(it) }
        } else {
            sorted
        }
        System.err.println("MelodyNews: 关键词过滤后 ${filtered.size} 条")

        filtered
    }

    /**
     * 翻译新闻列表中的英文内容（并发翻译加速）
     * 只翻译检测为英文的标题和摘要，中文内容保持原样
     */
    private suspend fun translateItems(items: List<NewsItem>): List<NewsItem> = coroutineScope {
        // 确保翻译模型就绪（首次会下载，约30MB）
        val modelReady = NewsTranslator.ensureReady()
        if (!modelReady) return@coroutineScope items  // 模型不可用，返回原文

        // 并发翻译每条（比串行快很多）
        items.map { item ->
            async { translateItem(item) }
        }.awaitAll()
    }

    /**
     * 翻译单条新闻
     */
    private suspend fun translateItem(item: NewsItem): NewsItem {
        val titleEn = item.title
        val summaryEn = item.summary

        val titleCn = if (NewsTranslator.isLikelyEnglish(titleEn)) {
            NewsTranslator.translate(titleEn)
        } else titleEn

        val summaryCn = if (summaryEn != titleEn && NewsTranslator.isLikelyEnglish(summaryEn)) {
            NewsTranslator.translate(summaryEn)
        } else summaryEn

        return item.copy(title = titleCn, summary = summaryCn)
    }

    /**
     * 标题归一化（用于去重）：转小写 + 去空格标点
     */
    private fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]"), "")
            .take(50)
    }

    /**
     * 诊断：测试国内 AI 源的连通性
     */
    private suspend fun testChineseSources() = withContext(Dispatchers.IO) {
        val sources = listOf(
            "https://36kr.com/feed" to "36kr",
            "https://www.jiqizhixin.com/rss" to "机器之心",
            "https://www.qbitai.com/feed" to "量子位",
            "https://juejin.cn" to "掘金",
            "https://www.chinaz.com" to "站长之家",
            "https://www.leiphone.com/feed" to "雷锋网"
        )
        for ((url, name) in sources) {
            try {
                val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    connectTimeout = 6000
                    readTimeout = 6000
                    instanceFollowRedirects = true
                }
                val code = conn.responseCode
                System.err.println("MelodyNews: $name ($url) -> HTTP $code")
                conn.disconnect()
            } catch (e: Exception) {
                System.err.println("MelodyNews: $name ($url) -> 失败 ${e.message}")
            }
        }
    }
}
