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
     * 抓取全部 AI 资讯（五源并发 + 英文翻译 + 关键词过滤）
     * @param context 用于关键词订阅（可为 null 表示不过滤）
     */
    suspend fun fetchAllNews(context: android.content.Context? = null): List<NewsItem> = coroutineScope {
        // 1. 并发抓取（精简为高质量源，砍掉不稳定的 RSSHub 和反爬的 GitHub）
        val hnDeferred = async {
            try { HackerNewsSource.fetch() } catch (_: Exception) { emptyList() }
        }
        val anthropicDeferred = async {
            try { AnthropicSource.fetch() } catch (_: Exception) { emptyList() }
        }
        val devtoDeferred = async {
            try { DevToSource.fetch() } catch (_: Exception) { emptyList() }
        }
        val techMediaDeferred = async {
            try { TechMediaSource.fetch() } catch (_: Exception) { emptyList() }
        }

        val hn = hnDeferred.await()
        val anthropic = anthropicDeferred.await()
        val devto = devtoDeferred.await()
        val techMedia = techMediaDeferred.await()

        System.err.println("MelodyNews: HN=${hn.size} Anthropic=${anthropic.size} DevTo=${devto.size} TechMedia=${techMedia.size}")

        // 2. 合并 + 去重
        val merged = (techMedia + hn + devto + anthropic)
            .distinctBy { normalizeTitle(it.title) }

        // 3. 翻译英文内容为中文
        val translated = translateItems(merged)

        // 4. 按来源分类排序（同类连续，播报时按板块过渡）
        val sourceOrder = listOf(
            // 顶级媒体优先
            NewsItem.SOURCE_TECHCRUNCH,
            NewsItem.SOURCE_VERGE,
            "WIRED",
            "VentureBeat",
            NewsItem.SOURCE_MIT,
            // 官方动态
            NewsItem.SOURCE_ANTHROPIC,
            // 技术社区
            NewsItem.SOURCE_HACKERNEWS,
            NewsItem.SOURCE_DEVTO,
            // 学术
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
     * 翻译新闻列表中的英文内容
     * 策略：LocalTranslator（本地术语字典，永远可用）优先
     *       MLKit（需连 Google 服务器）作为补充
     */
    private suspend fun translateItems(items: List<NewsItem>): List<NewsItem> = coroutineScope {
        // 先用本地术语字典翻译（永远可用，无需网络）
        val locallyTranslated = items.map { item ->
            async {
                val titleCn = LocalTranslator.translateEnglish(item.title)
                val summaryCn = LocalTranslator.translateEnglish(item.summary)
                item.copy(title = titleCn, summary = summaryCn)
            }
        }.awaitAll()

        // 尝试 MLKit 补充（翻译本地字典没覆盖的句子）
        try {
            val modelReady = NewsTranslator.ensureReady()
            if (modelReady) {
                locallyTranslated.map { item ->
                    async { translateItemWithMLKit(item) }
                }.awaitAll()
            } else {
                locallyTranslated
            }
        } catch (_: Exception) {
            // MLKit 不可用，返回本地翻译结果
            locallyTranslated
        }
    }

    /**
     * MLKit 补充翻译（仅翻译本地字典没覆盖的内容）
     */
    private suspend fun translateItemWithMLKit(item: NewsItem): NewsItem {
        val title = item.title
        val summary = item.summary

        val titleCn = if (LocalTranslator.isMostlyEnglish(title)) {
            NewsTranslator.translate(title)
        } else title

        val summaryCn = if (summary != title && LocalTranslator.isMostlyEnglish(summary)) {
            NewsTranslator.translate(summary)
        } else summary

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
}
