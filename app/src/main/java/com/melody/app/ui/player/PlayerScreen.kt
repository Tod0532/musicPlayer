package com.melody.app.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melody.app.domain.model.LyricLine
import com.melody.app.domain.model.PlayMode
import com.melody.app.domain.model.PlayState
import com.melody.app.domain.model.Song
import com.melody.app.ui.CoverPlaceholder
import com.melody.app.ui.formatDuration
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * 播放器全屏页
 */
@Composable
fun PlayerScreen(
    song: Song,
    playState: PlayState,
    playMode: PlayMode,
    currentPosition: Long,
    lyrics: List<LyricLine>,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCycleMode: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onLyricClick: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(song.coverColor).copy(alpha = 0.4f),
                        Color(0xFF0F0F1E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            PlayerTopBar(song = song, onClose = onClose)
            Spacer(modifier = Modifier.height(24.dp))

            // 封面/歌词双视图（点击切换）
            var showFullLyrics by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

            if (!showFullLyrics) {
                // 封面模式
                CoverWithRotation(
                    song = song,
                    isPlaying = playState == PlayState.PLAYING,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { showFullLyrics = true }
                )
            } else {
                // 完整歌词模式
                FullLyricsView(
                    lyrics = lyrics,
                    currentPosition = currentPosition,
                    onLyricClick = { time ->
                        onLyricClick(time)
                    },
                    onClose = { showFullLyrics = false },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(if (showFullLyrics) 8.dp else 32.dp))

            SongInfoRow(
                song = song,
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite
            )
            Spacer(modifier = Modifier.height(20.dp))

            ProgressBar(
                position = currentPosition,
                duration = song.duration,
                onSeek = onSeek
            )
            Spacer(modifier = Modifier.height(12.dp))

            ControlRow(
                playState = playState,
                playMode = playMode,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onCycleMode = onCycleMode
            )
            if (!showFullLyrics) {
                Spacer(modifier = Modifier.weight(1f))
                // 底部歌词预览（三行）
                LyricsPreview(
                    lyrics = lyrics,
                    currentPosition = currentPosition,
                    onLyricClick = onLyricClick
                )
            }
        }
    }
}

/**
 * 完整歌词视图（自动滚动 + 当前行高亮 + 点击跳转）
 */
@Composable
private fun FullLyricsView(
    lyrics: List<LyricLine>,
    currentPosition: Long,
    onLyricClick: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeIndex = lyrics.indexOfLast { it.time <= currentPosition }.coerceAtLeast(0)
    val listState = rememberLazyListState()

    // 自动滚动到当前行（居中）
    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && activeIndex >= 0) {
            listState.animateScrollToItem(index = activeIndex.coerceIn(0, lyrics.size - 1))
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(lyrics.size) { index ->
                val lyric = lyrics[index]
                val isActive = index == activeIndex
                val distance = kotlin.math.abs(index - activeIndex)
                // 距离当前行越远越淡
                val alpha = when {
                    isActive -> 1f
                    distance <= 2 -> 0.5f
                    distance <= 5 -> 0.25f
                    else -> 0.12f
                }
                Text(
                    text = lyric.text,
                    fontSize = if (isActive) 18.sp else 15.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = alpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .clickable { onLyricClick(lyric.time) }
                )
            }
        }
        // 提示：点击歌词跳转，点击空白区切回封面
        Text(
            text = "点击歌词跳转 · 点击封面切回",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun PlayerTopBar(song: Song, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "关闭",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onClose)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "正在播放",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = Icons.Filled.MoreHoriz,
            contentDescription = "更多",
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .clickable { }
        )
    }
}

@Composable
private fun CoverWithRotation(song: Song, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "cover")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val actualRotation = if (isPlaying) rotation else 0f

    CoverPlaceholder(
        startColor = Color(song.coverColor),
        endColor = Color(song.coverColor2),
        cornerRadius = 24.dp,
        iconSize = 80.dp,
        coverUri = song.coverUri,
        modifier = modifier
            .size(260.dp)
            .shadow(
                elevation = 30.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color(song.coverColor).copy(alpha = 0.5f),
                spotColor = Color(song.coverColor2).copy(alpha = 0.6f)
            )
            .rotate(actualRotation)
    )
}

@Composable
private fun SongInfoRow(song: Song, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = "收藏",
            tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(26.dp)
                .clickable(onClick = onToggleFavorite)
        )
    }
}

/**
 * 简洁可靠的进度条：容器根据播放进度填充宽度
 */
@Composable
private fun ProgressBar(position: Long, duration: Long, onSeek: (Long) -> Unit) {
    val rawProgress = if (duration > 0) (position.toFloat() / duration) else 0f
    val playerProgress = if (rawProgress.isFinite()) rawProgress.coerceIn(0f, 1f) else 0f

    // 拖动状态：非 null 表示正在拖动，值是拖动中的进度
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val displayProgress = dragProgress ?: playerProgress
    val displayPosition = (duration * displayProgress).toLong()

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(duration) {
                    val trackWidthPx = size.width.toFloat()
                    // 点击 seek
                    detectTapGestures(
                        onTap = { offset ->
                            if (trackWidthPx > 0 && duration > 0) {
                                val ratio = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                                dragProgress = null
                                onSeek((duration * ratio).toLong())
                            }
                        }
                    )
                }
                .pointerInput(duration) {
                    val trackWidthPx = size.width.toFloat()
                    // 拖拽 seek
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (trackWidthPx > 0 && duration > 0) {
                                dragProgress = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            }
                        },
                        onHorizontalDrag = { change, _ ->
                            if (trackWidthPx > 0 && duration > 0) {
                                dragProgress = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            dragProgress?.let { onSeek((duration * it).toLong()) }
                            dragProgress = null
                        },
                        onDragCancel = { dragProgress = null }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // 轨道背景
            Box(
                modifier = Modifier.fillMaxWidth().height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            )
            // 已播放部分（拖动时也跟随手指）
            Box(
                modifier = Modifier.fillMaxWidth(displayProgress).height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            // 拖动点（拖动时放大）
            Box(
                modifier = Modifier
                    .padding(start = (displayProgress * 0).dp)
                    .size(if (dragProgress != null) 14.dp else 10.dp)
                    .align(Alignment.CenterStart)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(0, placeable.height) {
                            val parentWidth = constraints.maxWidth.toFloat()
                            val x = parentWidth * displayProgress - placeable.width / 2f
                            placeable.placeRelative(x.coerceAtLeast(0f).toInt(), 0)
                        }
                    }
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayPosition),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = if (dragProgress != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ControlRow(
    playState: PlayState,
    playMode: PlayMode,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onCycleMode: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (playMode) {
                PlayMode.SEQUENCE -> Icons.Filled.Repeat
                PlayMode.SHUFFLE -> Icons.Filled.Shuffle
                PlayMode.REPEAT_ONE -> Icons.Filled.RepeatOne
            },
            contentDescription = "播放模式",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onCycleMode)
        )
        Icon(
            imageVector = Icons.Filled.SkipPrevious,
            contentDescription = "上一首",
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onPrevious)
        )
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (playState == PlayState.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "播放/暂停",
                tint = Color(0xFF1A0F2E),
                modifier = Modifier.size(28.dp)
            )
        }
        Icon(
            imageVector = Icons.Filled.SkipNext,
            contentDescription = "下一首",
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onNext)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = "队列",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(22.dp)
                .clickable { }
        )
    }
}

@Composable
private fun LyricsPreview(
    lyrics: List<LyricLine>,
    currentPosition: Long,
    onLyricClick: (Long) -> Unit
) {
    val activeIndex = lyrics.indexOfLast { it.time <= currentPosition }.coerceAtLeast(0)
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty()) {
            listState.animateScrollToItem(index = (activeIndex - 1).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        userScrollEnabled = false
    ) {
        items(lyrics.size) { index ->
            val lyric = lyrics[index]
            val isActive = index == activeIndex
            Text(
                text = lyric.text,
                fontSize = if (isActive) 14.sp else 12.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clickable { onLyricClick(lyric.time) }
            )
        }
    }
}
