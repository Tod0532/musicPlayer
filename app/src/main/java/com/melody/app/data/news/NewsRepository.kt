package com.melody.app.data.news

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 资讯聚合仓库
 *
 * 并发抓取三个数据源，合并去重排序
 */
object NewsRepository {

    /**
     * 抓取全部 AI 资讯（三源并发）
     *
     * @return 合并去重后的资讯列表（按时间倒序）
     */
    suspend fun fetchAllNews(): List<NewsItem> = coroutineScope {
        // 并发抓取三个源
        val hnDeferred = async { HackerNewsSource.fetch() }
        val ghDeferred = async { GitHubTrendingSource.fetch() }
        val rssDeferred = async { RSSHubSource.fetch() }

        val hn = hnDeferred.await()
        val gh = ghDeferred.await()
        val rss = rssDeferred.await()

        // 合并 + 去重 + 排序
        (hn + gh + rss)
            .distinctBy { normalizeTitle(it.title) }   // 按标题去重
            .sortedByDescending { it.publishedAt }      // 按时间倒序
    }

    /**
     * 标题归一化（用于去重）：转小写 + 去空格标点
     */
    private fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]"), "")  // 只保留字母数字中文
            .take(50)  // 取前50字符避免长标题误判
    }
}
