package com.melody.app.ui.mymusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.melody.app.domain.model.Song
import com.melody.app.ui.CoverPlaceholder
import com.melody.app.ui.formatDuration

/**
 * 我的音乐页（歌曲列表）
 */
@Composable
fun MyMusicScreen(
    songs: List<Song>,
    currentIndex: Int,
    onSongClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isScanning: Boolean = false,
    onScanClick: () -> Unit = {}
) {
    // 本地搜索过滤（纯 UI 状态）
    var localQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val filteredSongs = androidx.compose.runtime.remember(songs, localQuery) {
        if (localQuery.isBlank()) songs
        else songs.filter {
            it.title.contains(localQuery, true) ||
            it.artist.contains(localQuery, true) ||
            it.album.contains(localQuery, true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // 顶部标题 + 扫描按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "我的音乐",
                style = MaterialTheme.typography.headlineLarge
            )
            // 扫描按钮
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable(onClick = onScanClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isScanning) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isScanning) "扫描中" else "扫描音乐",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Text(
            text = when {
                isScanning -> "正在扫描本地音乐..."
                localQuery.isNotBlank() -> "筛选结果 ${filteredSongs.size} 首"
                songs.isNotEmpty() -> "共 ${songs.size} 首歌曲"
                else -> "暂无音乐"
            },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
        )

        // 本地搜索框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔍",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = localQuery,
                onValueChange = { localQuery = it },
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                decorationBox = { inner ->
                    if (localQuery.isEmpty()) {
                        Text(
                            text = "筛选本地歌曲",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    inner()
                }
            )
            if (localQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { localQuery = "" }
                )
            }
        }

        // Tab（静态展示）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            TabItem("歌曲", true)
            Spacer(modifier = Modifier.width(24.dp))
            TabItem("我喜欢", false)
            Spacer(modifier = Modifier.width(24.dp))
            TabItem("最近", false)
        }

        HorizontalDivider()

        // 歌曲列表（用过滤后的）
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 6.dp)
        ) {
            items(filteredSongs.size) { index ->
                val song = filteredSongs[index]
                // 找到原始 songs 中的索引（保证点击播放正确的歌）
                val originalIndex = songs.indexOf(song)
                SongRow(
                    song = song,
                    isPlaying = originalIndex == currentIndex,
                    onClick = { onSongClick(originalIndex) }
                )
            }
            // 空结果提示
            if (filteredSongs.isEmpty() && localQuery.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到匹配的歌曲",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabItem(label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = if (active) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(if (active) 28.dp else 0.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

@Composable
private fun SongRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            CoverPlaceholder(
                startColor = Color(song.coverColor),
                endColor = Color(song.coverColor2),
                cornerRadius = 10.dp,
                iconSize = 20.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(10.dp),
                        clip = false
                    )
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1
                )
            }
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(end = 8.dp)
        )

        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "更多",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier
                .size(20.dp)
                .clickable { }
        )
    }
}
