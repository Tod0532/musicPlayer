package com.melody.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.melody.app.data.MusicScanner
import com.melody.app.data.SampleData
import com.melody.app.data.local.MelodyDatabase
import com.melody.app.data.local.PlaylistEntity
import com.melody.app.data.local.PlaylistSongEntity
import com.melody.app.data.online.OnlineMusicSource
import com.melody.app.domain.model.LyricLine
import com.melody.app.media.LyricsParser
import com.melody.app.domain.model.PlayMode
import com.melody.app.domain.model.PlayState
import com.melody.app.domain.model.Song
import com.melody.app.media.MediaServiceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 播放器 UI 状态
 */
data class PlayerUiState(
    val songs: List<Song> = SampleData.songs,   // 默认显示内置示例
    val currentIndex: Int = 0,
    val playState: PlayState = PlayState.PAUSED,
    val playMode: PlayMode = PlayMode.SEQUENCE,
    val currentPosition: Long = 0L,     // 毫秒
    val duration: Long = 0L,            // 当前歌曲真实时长
    val isFullScreenPlayer: Boolean = false,
    val lyrics: List<LyricLine> = SampleData.sampleLyrics,
    val isScanning: Boolean = false,    // 是否正在扫描
    val hasPermission: Boolean = false, // 是否已获得读取权限
    val searchQuery: String = "",       // 在线搜索关键词
    val searchResults: List<com.melody.app.data.online.SearchResult> = emptyList(),  // 搜索结果（带播放链接）
    val isSearching: Boolean = false,   // 是否正在搜索
    val currentTab: Int = 0,            // 当前 Tab（0=我的, 1=搜索, 2=歌单）
    val playlists: List<PlaylistEntity> = emptyList(),  // 歌单列表
    val playlistSongs: List<PlaylistSongEntity> = emptyList(),  // 当前歌单内的歌曲
    val viewingPlaylistId: Long? = null,  // 正在查看的歌单 ID（null=歌单列表页）
    // ---- 资讯播报 ----
    val newsItems: List<com.melody.app.data.news.NewsItem> = emptyList(),  // 新闻列表
    val isFetchingNews: Boolean = false,    // 是否正在抓取新闻
    val isNewsMode: Boolean = false,         // 是否处于新闻播报模式
    val newsIndex: Int = 0,                  // 当前播报的新闻索引
    val isNewsPlaying: Boolean = false,      // 新闻是否正在朗读
    val isNewsFullScreen: Boolean = false    // 是否展开播报详情页
) {
    val currentSong: Song get() = songs.getOrElse(currentIndex) { songs.firstOrNull() ?: Song(0, "", "", "", 0L) }
    val progress: Float
        get() {
            val d = if (duration > 0) duration else currentSong.duration
            val p = if (d > 0) currentPosition.toFloat() / d else 0f
            return if (p.isFinite()) p.coerceIn(0f, 1f) else 0f
        }
}

/**
 * 主 ViewModel（接 Media3 真实播放 + MediaStore 扫描）
 */
class PlayerViewModel(private val application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // 用 MediaServiceConnection 连接后台 Service（实现后台播放+通知栏）
    private val mediaConnection = MediaServiceConnection(application).also { conn ->
        conn.onPlaybackStateChanged = { isPlaying, position, duration ->
            val state = _uiState.value
            _uiState.value = state.copy(
                playState = if (isPlaying) PlayState.PLAYING else PlayState.PAUSED,
                currentPosition = position,
                duration = duration
            )
        }
        conn.onPlaybackCompleted = {
            playNext()
        }
        // 通知栏/锁屏点击下一首/上一首时，Player 队列自动切换，同步 UI 当前歌曲
        conn.onQueueIndexChanged = { newIndex ->
            val state = _uiState.value
            val songs = state.songs
            if (newIndex in songs.indices) {
                val song = songs[newIndex]
                val lyrics = if (song.lrcAsset != null) {
                    LyricsParser.parseFromAssets(application, song.lrcAsset)
                } else {
                    listOf(LyricLine(0, "暂无歌词"))
                }
                _uiState.value = state.copy(
                    currentIndex = newIndex,
                    currentPosition = 0L,
                    duration = song.duration,
                    lyrics = lyrics
                )
            }
        }
    }

    // Room 数据库（歌单管理）
    private val database = MelodyDatabase.getInstance(application)
    private val playlistDao = database.playlistDao()

    private var progressJob: kotlinx.coroutines.Job? = null

    init {
        // 连接到后台播放服务
        mediaConnection.connect()
        startProgressPolling()
        // 观察歌单列表变化
        viewModelScope.launch {
            playlistDao.observePlaylists().collect { playlists ->
                _uiState.value = _uiState.value.copy(playlists = playlists)
            }
        }
    }

    // ---- 歌单管理 ----

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlist)
        }
    }

    /**
     * 将当前播放的歌曲添加到指定歌单
     */
    fun addCurrentSongToPlaylist(playlistId: Long) {
        val song = _uiState.value.currentSong
        viewModelScope.launch {
            playlistDao.addSongToPlaylist(
                PlaylistSongEntity(
                    playlistId = playlistId,
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    duration = song.duration,
                    coverColor = song.coverColor,
                    coverColor2 = song.coverColor2,
                    audioAsset = song.audioAsset,
                    mediaUri = song.mediaUri
                )
            )
        }
    }

    /**
     * 打开歌单详情（观察歌单内歌曲）
     */
    private var playlistSongsJob: kotlinx.coroutines.Job? = null

    fun openPlaylistDetail(playlistId: Long) {
        // 取消上一个歌单的观察
        playlistSongsJob?.cancel()
        _uiState.value = _uiState.value.copy(viewingPlaylistId = playlistId, playlistSongs = emptyList())
        playlistSongsJob = viewModelScope.launch {
            playlistDao.observeSongsInPlaylist(playlistId).collect { songs ->
                _uiState.value = _uiState.value.copy(playlistSongs = songs)
            }
        }
    }

    /**
     * 返回歌单列表（关闭详情）
     */
    fun closePlaylistDetail() {
        playlistSongsJob?.cancel()
        _uiState.value = _uiState.value.copy(viewingPlaylistId = null, playlistSongs = emptyList())
    }

    /**
     * 播放歌单内的歌曲
     */
    fun playPlaylistSong(index: Int) {
        val state = _uiState.value
        val ps = state.playlistSongs.getOrNull(index) ?: return
        // 转为 Song 并设为播放列表
        val songs = state.playlistSongs.map { it.toSong() }
        _uiState.value = state.copy(
            songs = songs,
            currentIndex = index,
            playState = PlayState.PLAYING,
            currentPosition = 0L,
            isFullScreenPlayer = true,
            duration = ps.duration,
            lyrics = listOf(LyricLine(0, "暂无歌词"))
        )
        when {
            ps.audioAsset != null -> mediaConnection.playAsset(ps.audioAsset)
            ps.mediaUri != null -> mediaConnection.playUri(ps.mediaUri)
            else -> mediaConnection.stop()
        }
    }

    /**
     * 从歌单移除歌曲
     */
    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
        }
    }

    /**
     * 检查并请求读取音频权限
     * 返回 true 表示已有权限
     */
    fun checkPermission(): Boolean {
        val granted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            application.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            application.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        _uiState.value = _uiState.value.copy(hasPermission = granted)
        return granted
    }

    /**
     * 扫描本地音乐（在 IO 线程执行）
     * 扫描结果与内置示例合并：扫描到的真实歌曲在前，示例在后
     */
    fun scanLocalMusic() {
        if (_uiState.value.isScanning) return
        if (!checkPermission()) return

        _uiState.value = _uiState.value.copy(isScanning = true)
        viewModelScope.launch {
            val scanned = withContext(Dispatchers.IO) {
                MusicScanner.scan(application)
            }
            // 合并：扫描到的真实歌曲 + 内置示例（示例作为兜底/演示）
            val merged = if (scanned.isNotEmpty()) scanned else SampleData.songs
            _uiState.value = _uiState.value.copy(
                songs = merged,
                isScanning = false
            )
        }
    }

    /**
     * 切换 Tab
     */
    fun switchTab(index: Int) {
        _uiState.value = _uiState.value.copy(currentTab = index)
    }

    /**
     * 在线搜索（开放音源）
     */
    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        // 延迟搜索（防抖），关键词非空时触发
        searchJob?.cancel()
        if (query.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(500)  // 防抖 500ms
                performSearch(query)
            }
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true)
        try {
            val results = OnlineMusicSource.searchWithPlayUrl(query)
            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isSearching = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isSearching = false)
        }
    }

    /**
     * 播放在线搜索结果中的歌曲（用返回的 playUrl 流式播放）
     */
    fun playSearchResult(index: Int) {
        val state = _uiState.value
        val results = state.searchResults
        val result = results.getOrNull(index) ?: return
        // 将搜索结果转为 Song 并设为播放列表
        val songs = results.map { it.toSong() }
        _uiState.value = state.copy(
            songs = songs,
            currentIndex = index,
            playState = PlayState.PLAYING,
            currentPosition = 0L,
            isFullScreenPlayer = true,
            duration = result.duration
        )
        // 用返回的 playUrl 流式播放
        mediaConnection.playUri(result.playUrl)
    }

    fun playSongAt(index: Int) {
        val state = _uiState.value
        val song = state.songs.getOrNull(index) ?: return
        // 加载对应歌词（如果有 LRC 文件）
        val lyrics = if (song.lrcAsset != null) {
            LyricsParser.parseFromAssets(application, song.lrcAsset)
        } else {
            // 无歌词文件时显示占位
            listOf(LyricLine(0, "暂无歌词"))
        }
        _uiState.value = state.copy(
            currentIndex = index,
            playState = PlayState.PLAYING,
            currentPosition = 0L,
            isFullScreenPlayer = true,
            duration = song.duration,
            lyrics = lyrics
        )
        // 设置完整播放队列（让通知栏/锁屏显示上一首/下一首按钮）
        val queue = state.songs.mapNotNull { s ->
            val uri = when {
                s.audioAsset != null -> "asset:///${s.audioAsset}"
                s.mediaUri != null -> s.mediaUri
                else -> return@mapNotNull null  // 跳过无音频源的
            }
            com.melody.app.media.QueueItem(
                uri = uri,
                title = s.title,
                artist = s.artist,
                album = s.album,
                coverUri = s.albumArtUri ?: s.coverUri,
                durationMs = s.duration
            )
        }
        // 队列中对应的索引（跳过无音频项后重新计数）
        val queueIndex = state.songs.subList(0, index).count { it.audioAsset != null || it.mediaUri != null }
        if (queue.isNotEmpty()) {
            mediaConnection.setQueueAndPlay(queue, queueIndex)
        }
    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state.playState == PlayState.PLAYING) {
            _uiState.value = state.copy(playState = PlayState.PAUSED)
            mediaConnection.pause()
        } else {
            // 恢复播放：如果当前歌曲有音频且未加载，重新播放
            val song = state.currentSong
            if (song.audioAsset != null && state.currentPosition == 0L) {
                mediaConnection.playAsset(song.audioAsset)
            } else {
                _uiState.value = state.copy(playState = PlayState.PLAYING)
                mediaConnection.play()
            }
        }
    }

    fun toggleFullScreenPlayer() {
        _uiState.value = _uiState.value.copy(isFullScreenPlayer = !_uiState.value.isFullScreenPlayer)
    }

    fun playNext() {
        // 走 Player 队列（通知栏下一首按钮也是这个路径）
        // UI 状态由 onQueueIndexChanged 回调自动同步
        mediaConnection.seekToNext()
    }

    fun playPrevious() {
        // 走 Player 队列（通知栏上一首按钮也是这个路径）
        mediaConnection.seekToPrevious()
    }

    fun cyclePlayMode() {
        val state = _uiState.value
        val next = when (state.playMode) {
            PlayMode.SEQUENCE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SEQUENCE
        }
        _uiState.value = state.copy(playMode = next)
        // 同步播放模式到 Player（影响通知栏循环/随机行为）
        mediaConnection.setPlayMode(next)
    }

    fun seekTo(position: Long) {
        _uiState.value = _uiState.value.copy(currentPosition = position)
        mediaConnection.seekTo(position)
    }

    fun closeFullScreenPlayer() {
        _uiState.value = _uiState.value.copy(isFullScreenPlayer = false)
    }

    fun toggleFavorite() {
        val state = _uiState.value
        val updated = state.songs.toMutableList()
        val cur = updated[state.currentIndex]
        updated[state.currentIndex] = cur.copy(isFavorite = !cur.isFavorite)
        _uiState.value = state.copy(songs = updated)
    }

    /**
     * 定时轮询真实播放进度（ExoPlayer 不主动推送 position 变化）
     */
    private fun startProgressPolling() {
        progressJob = viewModelScope.launch {
            while (true) {
                delay(300)
                val state = _uiState.value
                if (state.playState == PlayState.PLAYING) {
                    val pos = mediaConnection.getCurrentPosition()
                    val dur = mediaConnection.getDuration()
                    // 只在有真实播放时更新位置（避免覆盖无音频歌曲的静态状态）
                    if (state.currentSong.audioAsset != null) {
                        _uiState.value = state.copy(
                            currentPosition = pos,
                            duration = if (dur > 0) dur else state.duration
                        )
                    }
                }
            }
        }
    }

    // ============ 资讯播报 ============

    private val newsPlayer = com.melody.app.media.NewsPlayerController(application).also { npc ->
        npc.onStateChanged = { isPlaying, index, total ->
            _uiState.value = _uiState.value.copy(
                isNewsPlaying = isPlaying,
                newsIndex = index
            )
        }
        npc.onCompleted = {
            // 全部播完，退出新闻模式
            _uiState.value = _uiState.value.copy(isNewsMode = false, isNewsPlaying = false)
        }
    }

    /**
     * 抓取 AI 资讯（并发三源）
     */
    fun fetchNews() {
        if (_uiState.value.isFetchingNews) return
        _uiState.value = _uiState.value.copy(isFetchingNews = true)
        viewModelScope.launch {
            val news = withContext(Dispatchers.IO) {
                com.melody.app.data.news.NewsRepository.fetchAllNews()
            }
            _uiState.value = _uiState.value.copy(
                newsItems = news,
                isFetchingNews = false
            )
        }
    }

    /**
     * 开始播报全部新闻（从第一条开始）
     */
    fun startNewsPlaybackAll() {
        val items = _uiState.value.newsItems
        if (items.isEmpty()) return
        // 互斥：暂停音乐
        mediaConnection.pause()
        _uiState.value = _uiState.value.copy(
            isNewsMode = true,
            isNewsFullScreen = true,
            newsIndex = 0
        )
        newsPlayer.startPlayback(items, 0)
    }

    /**
     * 播报单条新闻
     */
    fun playNewsSingle(index: Int) {
        val items = _uiState.value.newsItems
        if (items.isEmpty() || index !in items.indices) return
        mediaConnection.pause()
        _uiState.value = _uiState.value.copy(
            isNewsMode = true,
            isNewsFullScreen = true,
            newsIndex = index
        )
        newsPlayer.startPlayback(items, index)
    }

    fun newsPlayPause() {
        if (_uiState.value.isNewsPlaying) newsPlayer.pause() else newsPlayer.play()
    }

    fun newsNext() { newsPlayer.next() }
    fun newsPrevious() { newsPlayer.previous() }

    fun closeNewsFullScreen() {
        _uiState.value = _uiState.value.copy(isNewsFullScreen = false)
    }

    /**
     * 停止新闻播报，切回音乐模式
     */
    fun stopNewsPlayback() {
        newsPlayer.stop()
        _uiState.value = _uiState.value.copy(
            isNewsMode = false,
            isNewsPlaying = false,
            isNewsFullScreen = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        mediaConnection.disconnect()
        newsPlayer.release()
    }
}
