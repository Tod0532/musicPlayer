package com.melody.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.melody.app.ui.theme.MelodyTheme
import com.melody.app.ui.PlayerViewModel
import com.melody.app.ui.components.MiniPlayer
import com.melody.app.ui.mymusic.MyMusicScreen
import com.melody.app.ui.player.PlayerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelodyTheme {
                MelodyApp()
            }
        }
    }
}

private data class TabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val tabs = listOf(
    TabItem("我的", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    TabItem("搜索", Icons.Filled.Search, Icons.Outlined.Search),
    TabItem("歌单", Icons.Filled.Explore, Icons.Outlined.Explore),
    TabItem("发现", Icons.Filled.Explore, Icons.Outlined.Explore)
)

@Composable
fun MelodyApp(viewModel: PlayerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column {
                    // 底部播放条（常驻）
                    MiniPlayer(
                        song = uiState.currentSong,
                        playState = uiState.playState,
                        progress = uiState.progress,
                        onClick = { viewModel.toggleFullScreenPlayer() },
                        onPlayPause = { viewModel.togglePlayPause() },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    // 底部导航
                    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = index == 0, // 演示：固定在第一个 Tab
                                onClick = { },
                                icon = {
                                    Icon(
                                        imageVector = if (index == 0) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                MyMusicScreen(
                    songs = uiState.songs,
                    currentIndex = uiState.currentIndex,
                    onSongClick = { index -> viewModel.playSongAt(index) }
                )
            }
        }

        // 播放器全屏页（浮层，带动画）
        AnimatedVisibility(
            visible = uiState.isFullScreenPlayer,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerScreen(
                song = uiState.currentSong,
                playState = uiState.playState,
                playMode = uiState.playMode,
                currentPosition = uiState.currentPosition,
                lyrics = uiState.lyrics,
                isFavorite = uiState.currentSong.isFavorite,
                onClose = { viewModel.closeFullScreenPlayer() },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onCycleMode = { viewModel.cyclePlayMode() },
                onSeek = { pos -> viewModel.seekTo(pos) },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onLyricClick = { time -> viewModel.seekTo(time) }
            )
        }
    }
}
