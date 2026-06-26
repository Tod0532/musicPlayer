package com.melody.app.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melody.app.data.local.PlaylistEntity
import com.melody.app.data.local.PlaylistSongEntity
import com.melody.app.domain.model.Song
import com.melody.app.ui.CoverPlaceholder
import com.melody.app.ui.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 歌单页（双模式：列表 / 详情）
 */
@Composable
fun PlaylistScreen(
    playlists: List<PlaylistEntity>,
    viewingPlaylist: PlaylistEntity?,
    playlistSongs: List<PlaylistSongEntity>,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onBackToList: () -> Unit,
    onPlaySong: (Int) -> Unit,
    onRemoveSong: (PlaylistEntity, Long) -> Unit,
    onAddCurrentSong: (PlaylistEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        if (viewingPlaylist == null) {
            PlaylistListMode(
                playlists = playlists,
                onCreate = { showCreateDialog = true },
                onPlaylistClick = onPlaylistClick,
                onDeletePlaylist = onDeletePlaylist,
                onAddCurrentSong = onAddCurrentSong
            )
        } else {
            PlaylistDetailMode(
                playlist = viewingPlaylist,
                songs = playlistSongs,
                onBack = onBackToList,
                onPlaySong = onPlaySong,
                onRemoveSong = { songId -> onRemoveSong(viewingPlaylist, songId) }
            )
        }
    }

    if (showCreateDialog && viewingPlaylist == null) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建歌单") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("输入歌单名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playlistName.isNotBlank()) onCreatePlaylist(playlistName)
                    showCreateDialog = false
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("取消") }
            }
        )
    }
}

// ---- 歌单列表模式 ----
@Composable
private fun ColumnScope.PlaylistListMode(
    playlists: List<PlaylistEntity>,
    onCreate: () -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onAddCurrentSong: (PlaylistEntity) -> Unit
) {
    var addTarget by remember { mutableStateOf<PlaylistEntity?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("歌单", style = MaterialTheme.typography.headlineLarge)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .clickable(onClick = onCreate)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("+ 新建", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
    Text("共 ${playlists.size} 个歌单",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))

    LazyColumn(modifier = Modifier.weight(1f)) {
        items(playlists) { playlist ->
            PlaylistRow(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) },
                onDelete = { onDeletePlaylist(playlist) },
                onAddCurrent = { addTarget = playlist }
            )
        }
    }

    if (playlists.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
            Text("还没有歌单，点击右上角创建",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }

    // "加入当前歌曲"对话框（简化：直接加入）
    addTarget?.let { playlist ->
        AlertDialog(
            onDismissRequest = { addTarget = null },
            title = { Text(playlist.name) },
            text = { Text("将当前播放的歌曲「加入」这个歌单？") },
            confirmButton = {
                TextButton(onClick = {
                    onAddCurrentSong(playlist)
                    addTarget = null
                }) { Text("加入") }
            },
            dismissButton = {
                TextButton(onClick = { addTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onAddCurrent: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Text("♪", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("创建于 ${dateFormat.format(Date(playlist.createdAt))}",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
        Text("+",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onAddCurrent).padding(8.dp))
        Icon(Icons.Filled.Delete, "删除",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp).clickable(onClick = onDelete))
    }
}

// ---- 歌单详情模式 ----
@Composable
private fun ColumnScope.PlaylistDetailMode(
    playlist: PlaylistEntity,
    songs: List<PlaylistSongEntity>,
    onBack: () -> Unit,
    onPlaySong: (Int) -> Unit,
    onRemoveSong: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp, 8.dp, 20.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹ 返回", color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
        Spacer(Modifier.weight(1f))
        Text(playlist.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
    Text("共 ${songs.size} 首",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 20.dp, bottom = 8.dp))

    LazyColumn(modifier = Modifier.weight(1f)) {
        items(songs.size) { index ->
            val ps = songs[index]
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onPlaySong(index) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverPlaceholder(
                    startColor = androidx.compose.ui.graphics.Color(ps.coverColor),
                    endColor = androidx.compose.ui.graphics.Color(ps.coverColor2),
                    cornerRadius = 8.dp, iconSize = 16.dp,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(ps.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
                    Text("${ps.artist} · ${formatDuration(ps.duration)}",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                }
                Icon(Icons.Filled.Delete, "移除",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp).clickable { onRemoveSong(ps.songId) })
            }
        }
    }

    if (songs.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
            Text("歌单是空的，在歌曲列表点 + 添加",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}
