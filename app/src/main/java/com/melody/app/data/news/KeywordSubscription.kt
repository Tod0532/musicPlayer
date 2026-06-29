package com.melody.app.data.news

import android.content.Context
import android.content.SharedPreferences

/**
 * 用户关键词订阅管理（SharedPreferences 持久化）
 *
 * 用户自定义关注的 AI 关键词，资讯抓取后按这些词过滤/标记。
 * 空列表 = 不过滤（显示全部）。
 */
class KeywordSubscription private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("keyword_subscription", Context.MODE_PRIVATE)

    /**
     * 获取用户订阅的关键词列表
     */
    fun getKeywords(): List<String> {
        val raw = prefs.getString(KEY_KEYWORDS, "") ?: ""
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    /**
     * 添加关键词
     */
    fun addKeyword(keyword: String): Boolean {
        val current = getKeywords().toMutableList()
        val normalized = keyword.trim()
        if (normalized.isBlank() || current.any { it.equals(normalized, ignoreCase = true) }) {
            return false  // 重复或空
        }
        current.add(normalized)
        prefs.edit().putString(KEY_KEYWORDS, current.joinToString(",")).apply()
        return true
    }

    /**
     * 移除关键词
     */
    fun removeKeyword(keyword: String) {
        val current = getKeywords().toMutableList()
        current.removeIf { it.equals(keyword, ignoreCase = true) }
        prefs.edit().putString(KEY_KEYWORDS, current.joinToString(",")).apply()
    }

    /**
     * 判断资讯是否匹配用户关键词
     * - 用户无关键词订阅 → 返回 true（不过滤，显示全部）
     * - 有关键词 → 标题/摘要含任一关键词则返回 true
     */
    fun matches(newsItem: NewsItem): Boolean {
        val keywords = getKeywords()
        if (keywords.isEmpty()) return true  // 无订阅，不过滤
        val text = "${newsItem.title} ${newsItem.summary}".lowercase()
        return keywords.any { kw -> text.contains(kw.lowercase()) }
    }

    companion object {
        private const val KEY_KEYWORDS = "subscribed_keywords"

        @Volatile
        private var INSTANCE: KeywordSubscription? = null

        fun getInstance(context: Context): KeywordSubscription {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KeywordSubscription(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 默认推荐关键词（新用户引导）
        val SUGGESTED_KEYWORDS = listOf(
            "GPT", "Claude", "Gemini", "LLM", "RAG", "Agent",
            "多模态", "文生图", "机器学习", "深度学习", "Stable Diffusion"
        )
    }
}
