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
     * 抓取全部 AI 资讯（三源并发 + 英文翻译）
     */
    suspend fun fetchAllNews(): List<NewsItem> = coroutineScope {
        // 1. 并发抓取三个源
        val hnDeferred = async {
            try {
                val r = HackerNewsSource.fetch()
                System.err.println("MelodyNews: HackerNews=${r.size}条")
                r
            } catch (e: Exception) {
                System.err.println("MelodyNews: HackerNews 失败: ${e.message}")
                emptyList()
            }
        }
        val ghDeferred = async {
            try {
                val r = GitHubTrendingSource.fetch()
                System.err.println("MelodyNews: GitHub=${r.size}条")
                r
            } catch (e: Exception) {
                System.err.println("MelodyNews: GitHub 失败: ${e.message}")
                emptyList()
            }
        }
        val rssDeferred = async {
            try {
                val r = RSSHubSource.fetch()
                System.err.println("MelodyNews: RSSHub=${r.size}条")
                r
            } catch (e: Exception) {
                System.err.println("MelodyNews: RSSHub 失败: ${e.message}")
                emptyList()
            }
        }

        val hn = hnDeferred.await()
        val gh = ghDeferred.await()
        val rss = rssDeferred.await()

        System.err.println("MelodyNews: 合计 HN=${hn.size} GH=${gh.size} RSS=${rss.size}")

        // 2. 合并 + 去重
        val merged = (hn + gh + rss)
            .distinctBy { normalizeTitle(it.title) }

        // 3. 翻译英文内容为中文
        val translated = translateItems(merged)

        // 4. 按时间倒序排序
        translated.sortedByDescending { it.publishedAt }
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
