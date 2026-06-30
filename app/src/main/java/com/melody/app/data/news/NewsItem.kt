package com.melody.app.data.news

/**
 * AI 资讯条目
 */
data class NewsItem(
    val id: String,           // 唯一标识（去重用）
    val title: String,        // 标题
    val summary: String,      // 摘要（标题+前几句，TTS 朗读内容）
    val source: String,       // 来源
    val url: String,          // 原文链接
    val publishedAt: Long,    // 发布时间戳（毫秒）
    val score: Int = 0,       // 热度
    val author: String = "",  // 作者
    val category: String = "" // 分类（如"开源项目""论文""博客"）
) {
    /**
     * 来源标识颜色（用于 UI 标签）
     */
    val sourceColor: Long get() = when (source) {
        SOURCE_HACKERNEWS -> 0xFFFF6600   // HN 橙
        SOURCE_GITHUB -> 0xFF34D399        // GitHub 绿
        SOURCE_JIQIZHIXIN -> 0xFF31C27C    // 机器之心 青
        SOURCE_QBITAI -> 0xFF31C27C        // 量子位 青
        SOURCE_ARXIV -> 0xFFA78BFA         // ArXiv 紫
        SOURCE_ANTHROPIC -> 0xFFD4A574      // Anthropic 棕
        SOURCE_36KR -> 0xFF2196F3            // 36kr 蓝
        SOURCE_DEVTO -> 0xFF60A5FA          // Dev.to 蓝
        SOURCE_TECHCRUNCH -> 0xFF00D100       // TechCrunch 绿
        SOURCE_VERGE -> 0xFFE847C7            // Verge 粉
        SOURCE_MIT -> 0xFFFF3B30              // MIT 红
        else -> 0xFF94A3B8
    }

    companion object {
        const val SOURCE_HACKERNEWS = "HackerNews"
        const val SOURCE_GITHUB = "GitHub"
        const val SOURCE_JIQIZHIXIN = "机器之心"
        const val SOURCE_QBITAI = "量子位"
        const val SOURCE_ARXIV = "ArXiv"
        const val SOURCE_ANTHROPIC = "Anthropic"
        const val SOURCE_36KR = "36氪"
        const val SOURCE_DEVTO = "Dev.to"
        const val SOURCE_TECHCRUNCH = "TechCrunch"
        const val SOURCE_VERGE = "The Verge"
        const val SOURCE_MIT = "MIT Tech Review"

        /**
         * AI 关键词过滤列表（小写匹配）
         */
        val AI_KEYWORDS = listOf(
            "ai", "a.i.", "ml", "gpt", "llm", "transformer", "diffusion",
            "claude", "gemini", "rag", "agent", "neural", "deep learning",
            "machine learning", "openai", "anthropic", "huggingface",
            "stable diffusion", "midjourney", "copilot", "prompt",
            "chatbot", "chatgpt", "bert", "embedding", "fine-tune", "finetune",
            "vision model", "language model", "generative", "agi"
        )

        /**
         * 判断文本是否与 AI 相关
         */
        fun isAiRelated(text: String): Boolean {
            val lower = text.lowercase()
            return AI_KEYWORDS.any { lower.contains(it) }
        }
    }
}
