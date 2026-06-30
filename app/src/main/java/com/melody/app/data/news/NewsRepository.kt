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

        // 4. 时效性过滤：只保留最近 48 小时的内容
        val cutoff = System.currentTimeMillis() - 48 * 3600 * 1000L
        val fresh = translated.filter { it.publishedAt > cutoff || it.publishedAt == 0L }

        // 5. 跨源去重合并：同一新闻在多个源出现时，保留质量最高的版本
        val deduplicated = mergeDuplicates(fresh)

        // 6. 质量评分排序（多维度打分）
        val scored = deduplicated.map { it to calculateQualityScore(it) }
            .sortedByDescending { it.second }
            .map { it.first }

        // 7. 中文优先：已翻译成中文的内容排在前面
        val chineseFirst = scored.sortedByDescending { item ->
            val title = item.title
            val cjkCount = title.count { it in '\u4e00'..'\u9fff' }
            val totalCount = title.length.coerceAtLeast(1)
            cjkCount.toFloat() / totalCount  // 中文占比越高越靠前
        }

        // 8. 关键词过滤（用户订阅了关键词则只保留匹配的）
        val keywordSubscription = context?.let { KeywordSubscription.getInstance(it) }
        val filtered = if (keywordSubscription != null) {
            chineseFirst.filter { keywordSubscription.matches(it) }
        } else {
            chineseFirst
        }

        // 9. 摘要增强：提取核心观点
        val enriched = filtered.map { item ->
            val corePoint = extractCorePoint(item.summary)
            item.copy(summary = if (corePoint.isNotBlank() && corePoint != item.summary) {
                "${item.summary}。核心观点：$corePoint"
            } else item.summary)
        }

        System.err.println("MelodyNews: 最终 ${enriched.size} 条（去重后 ${deduplicated.size}，时效过滤后 ${fresh.size}）")
        enriched
    }

    /**
     * 质量评分：多维度打分（0-100）
     * 来源权重(40%) + 时效性(30%) + 互动数(20%) + 中文质量(10%)
     */
    private fun calculateQualityScore(item: NewsItem): Float {
        // 来源权重（顶级媒体最高）
        val sourceWeight = when (item.source) {
            NewsItem.SOURCE_TECHCRUNCH -> 40f
            NewsItem.SOURCE_VERGE -> 38f
            "WIRED" -> 36f
            "VentureBeat" -> 34f
            NewsItem.SOURCE_ANTHROPIC -> 32f
            NewsItem.SOURCE_HACKERNEWS -> 25f
            NewsItem.SOURCE_DEVTO -> 20f
            NewsItem.SOURCE_ARXIV -> 15f
            else -> 10f
        }

        // 时效性（越新越高，48小时内线性衰减）
        val ageHours = (System.currentTimeMillis() - item.publishedAt) / 3_600_000f
        val freshness = when {
            ageHours < 0 -> 30f  // 无时间戳
            ageHours < 6 -> 30f
            ageHours < 24 -> 25f
            ageHours < 48 -> 15f
            else -> 5f
        }

        // 互动数（score 对数缩放）
        val engagement = when {
            item.score > 1000 -> 20f
            item.score > 100 -> 15f
            item.score > 10 -> 10f
            item.score > 0 -> 5f
            else -> 0f
        }

        // 中文质量（标题中文占比）
        val title = item.title
        val cjkRatio = if (title.isNotEmpty()) {
            title.count { it in '\u4e00'..'\u9fff' }.toFloat() / title.length
        } else 0f
        val chineseQuality = cjkRatio * 10f

        return sourceWeight + freshness + engagement + chineseQuality
    }

    /**
     * 跨源去重合并：同一新闻在多个源出现时，保留质量最高的版本
     * 策略：标题相似度 > 80% 视为同一新闻
     */
    private fun mergeDuplicates(items: List<NewsItem>): List<NewsItem> {
        if (items.size <= 1) return items

        val groups = mutableListOf<MutableList<NewsItem>>()
        val used = BooleanArray(items.size)

        for (i in items.indices) {
            if (used[i]) continue
            val group = mutableListOf(items[i])
            used[i] = true

            for (j in i + 1 until items.size) {
                if (used[j]) continue
                if (isSameStory(items[i], items[j])) {
                    group.add(items[j])
                    used[j] = true
                }
            }
            groups.add(group)
        }

        // 每组保留质量最高的版本
        return groups.map { group ->
            group.maxByOrNull { calculateQualityScore(it) } ?: group.first()
        }
    }

    /**
     * 判断两条新闻是否是同一故事
     * 标题相似度 > 60% 视为同一新闻
     */
    private fun isSameStory(a: NewsItem, b: NewsItem): Boolean {
        val titleA = normalizeTitle(a.title)
        val titleB = normalizeTitle(b.title)
        if (titleA == titleB) return true

        // 简单相似度：共同字符数 / 较长标题长度
        val commonChars = titleA.toSet().intersect(titleB.toSet()).size
        val maxLength = maxOf(titleA.length, titleB.length, 1)
        val similarity = commonChars.toFloat() / maxLength

        return similarity > 0.6f
    }

    /**
     * 从摘要中提取核心观点（第一个完整句子）
     */
    private fun extractCorePoint(summary: String): String {
        if (summary.isBlank()) return ""
        // 取第一个句号/感叹号/问号前的内容
        val firstSentence = summary.split(Regex("[。！？.!?]"))[0].trim()
        return if (firstSentence.length > 10) firstSentence else summary.take(100)
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
     * 标题归一化（用于去重）：转小写 + 去空格标点 + 取核心词
     * 改进：去除常见前缀后缀，提高跨源去重准确率
     */
    private fun normalizeTitle(title: String): String {
        var t = title.lowercase()
            .replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]"), "")  // 只保留字母数字中文
        // 去除常见前缀后缀（如"【新项目】"、"opinion:"等）
        t = t.replace(Regex("^(新项目|opinion|review|howto|howto)"), "")
        return t.take(60)  // 取前60字符（比之前50更长，提高匹配率）
    }
}
