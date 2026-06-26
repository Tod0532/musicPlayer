package com.melody.app.ui.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melody.app.data.news.NewsItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI 资讯列表页
 */
@Composable
fun NewsScreen(
    newsItems: List<NewsItem>,
    isFetching: Boolean,
    onRefresh: () -> Unit,
    onStartPlaybackAll: () -> Unit,
    onPlaySingle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = androidx.compose.runtime.remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val lastUpdate = androidx.compose.runtime.remember(newsItems) {
        if (newsItems.isNotEmpty()) "更新于 ${dateFormat.format(Date(newsItems.maxOf { it.publishedAt }))}"
        else ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📰 AI 资讯", style = MaterialTheme.typography.headlineLarge)
            // 刷新按钮
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text("⟳", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                }
                Text(" 刷新", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            text = when {
                isFetching -> "正在抓取最新资讯..."
                newsItems.isNotEmpty() -> "$lastUpdate · ${newsItems.size} 条"
                else -> "暂无资讯，点击刷新"
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
        )

        // 开始播报全部按钮
        if (newsItems.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onStartPlaybackAll)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("▶ 开始播报全部（${newsItems.size} 条）",
                    color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // 新闻列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(newsItems.size) { index ->
                val item = newsItems[index]
                NewsCard(
                    item = item,
                    onPlay = { onPlaySingle(index) },
                    onClick = {
                        // 打开原文链接
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                )
            }
            // 首次进入/加载中的空状态
            if (newsItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("正在抓取 AI 资讯...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 12.dp))
                            } else {
                                Text("📰", fontSize = 40.sp)
                                Text("点击右上角刷新，获取最新 AI 资讯",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(
    item: NewsItem,
    onPlay: () -> Unit,
    onClick: () -> Unit
) {
    val timeAgo = formatTimeAgo(item.publishedAt)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 来源标签
            Text(
                text = item.source,
                style = MaterialTheme.typography.bodySmall,
                color = Color(item.sourceColor),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(item.sourceColor).copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Spacer(Modifier.width(8.dp))
            // 热度
            if (item.score > 0) {
                Text(
                    text = "🔥 ${item.score}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.weight(1f))
            // 单条播放按钮
            Text(
                text = "▶",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onPlay).padding(4.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        // 标题
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
        // 摘要
        if (item.summary.isNotBlank() && item.summary != item.title) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = timeAgo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val hours = diff / 3_600_000
    val minutes = diff / 60_000
    return when {
        hours >= 24 -> "${hours / 24}天前"
        hours >= 1 -> "${hours}小时前"
        minutes >= 1 -> "${minutes}分钟前"
        else -> "刚刚"
    }
}
