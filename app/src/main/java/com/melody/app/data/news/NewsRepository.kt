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
     * 抓取全部 AI 资讯（纯中文源）
     * 只保留原生中文内容源，移除英文源
     */
    suspend fun fetchAllNews(context: android.content.Context? = null): List<NewsItem> = coroutineScope {
        // 只抓取中文源
        val qbitaiDeferred = async {
            try { QbitaiSource.fetch() } catch (_: Exception) { emptyList() }
        }
        // 从 assets 加载预抓取的中文内容
        val staticDeferred = async { StaticNewsSource.fetch(context) }

        val qbitai = qbitaiDeferred.await()
        val static = staticDeferred.await()

        System.err.println("MelodyNews: 量子位=${qbitai.size} 预加载=${static.size}")

        // 合并 + 去重
        val merged = (qbitai + static)
            .distinctBy { normalizeTitle(it.title) }

        // 不需要翻译（全是中文）
        // 时效性过滤（放宽到7天，因为预加载内容可能不是最新的）
        val cutoff = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
        val fresh = merged.filter { it.publishedAt > cutoff || it.publishedAt == 0L }

        // 去重合并
        val deduplicated = mergeDuplicates(fresh)

        // 质量评分排序
        val scored = deduplicated.map { it to calculateQualityScore(it) }
            .sortedByDescending { it.second }
            .map { it.first }

        // 关键词过滤
        val keywordSubscription = context?.let { KeywordSubscription.getInstance(it) }
        val filtered = if (keywordSubscription != null) {
            scored.filter { keywordSubscription.matches(it) }
        } else {
            scored
        }

        // 摘要增强
        val enriched = filtered.map { item ->
            val corePoint = extractCorePoint(item.summary)
            item.copy(summary = if (corePoint.isNotBlank() && corePoint != item.summary) {
                "${item.summary}。核心观点：$corePoint"
            } else item.summary)
        }

        System.err.println("MelodyNews: 最终 ${enriched.size} 条")
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
        val firstSentence = summary.split(Regex("[。！？.!?]"))[0].trim()
        return if (firstSentence.length > 10) firstSentence else summary.take(100)
    }

    /**
     * 清理翻译输出
     * - 去除多余空格（MLKit 逐词翻译会在每个词之间加空格）
     * - 格式化标点（中英文标点统一）
     * - 去除首尾空格
     */
    private fun cleanTranslatedText(text: String): String {
        if (text.isBlank()) return text
        var result = text
        // 去除中文字符之间的空格（"升级 到 我们" → "升级到我们"）
        result = result.replace(Regex("(?<=[\\u4e00-\\u9fff])\\s+(?=[\\u4e00-\\u9fff])"), "")
        // 去除中文标点前后的空格
        result = result.replace(Regex("\\s*([，。！？、；：])\\s*"), "$1")
        // 去除英文标点前后的多余空格
        result = result.replace(Regex("\\s*([,.!?;:])\\s*"), "$1 ")
        // 去除括号内的多余空格
        result = result.replace(Regex("\\(\\s+"), "(")
        result = result.replace(Regex("\\s+\\)"), ")")
        // 去除连续空格
        result = result.replace(Regex("\\s+"), " ")
        // 去除首尾空格
        result = result.trim()
        return result
    }

    /**
     * 翻译新闻列表中的英文内容
     * 策略：MyMemory API 尝试（可能不可达），失败保持原文
     * 不再用 LocalTranslator 乱翻译（半中半英比原文更难看）
     */
    private suspend fun translateItems(items: List<NewsItem>): List<NewsItem> = coroutineScope {
        items.map { item ->
            async { translateItem(item) }
        }.awaitAll()
    }

    private suspend fun translateItem(item: NewsItem): NewsItem {
        // 中文内容不需要翻译
        if (!CloudTranslator.needsTranslation(item.title)) return item

        // 尝试 MyMemory API 翻译（可能不可达）
        val titleCn = CloudTranslator.translate(item.title)
        val summaryCn = if (item.summary != item.title && CloudTranslator.needsTranslation(item.summary)) {
            CloudTranslator.translate(item.summary)
        } else item.summary

        // 如果 MyMemory 翻译成功了（返回值不等于原文），用翻译结果
        // 如果失败了（返回值等于原文），保持原文，不乱翻译
        return item.copy(title = titleCn, summary = summaryCn)
    }

    /**
     * MLKit 翻译单条新闻（质量更高）
     */
    private suspend fun translateItemWithMLKit(item: NewsItem): NewsItem {
        val title = item.title
        val summary = item.summary

        val titleCn = NewsTranslator.translate(title)
        val summaryCn = if (summary != title) NewsTranslator.translate(summary) else summary

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
