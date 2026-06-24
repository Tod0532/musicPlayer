package com.melody.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melody.app.data.SampleData
import com.melody.app.domain.model.LyricLine
import com.melody.app.domain.model.PlayMode
import com.melody.app.domain.model.PlayState
import com.melody.app.domain.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 播放器 UI 状态
 */
data class PlayerUiState(
    val songs: List<Song> = SampleData.songs,
    val currentIndex: Int = 0,
    val playState: PlayState = PlayState.PAUSED,
    val playMode: PlayMode = PlayMode.SEQUENCE,
    val currentPosition: Long = 0L,     // 毫秒
    val isFullScreenPlayer: Boolean = false,
    val lyrics: List<LyricLine> = SampleData.sampleLyrics
) {
    val currentSong: Song get() = songs.getOrElse(currentIndex) { songs.first() }
    val progress: Float
        get() = if (currentSong.duration > 0) {
            currentPosition.toFloat() / currentSong.duration
        } else 0f
}

/**
 * 主 ViewModel（模拟播放，真实 APP 接 Media3）
 */
class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: kotlinx.coroutines.Job? = null

    fun playSongAt(index: Int) {
        progressJob?.cancel()
        _uiState.value = _uiState.value.copy(
            currentIndex = index,
            playState = PlayState.PLAYING,
            currentPosition = 0L
        )
        startProgressSimulation()
    }

    fun togglePlayPause() {
        val state = _uiState.value
        val newState = if (state.playState == PlayState.PLAYING) PlayState.PAUSED else PlayState.PLAYING
        _uiState.value = state.copy(playState = newState)
        if (newState == PlayState.PLAYING) startProgressSimulation() else progressJob?.cancel()
    }

    fun playNext() {
        val state = _uiState.value
        val nextIndex = when (state.playMode) {
            PlayMode.SHUFFLE -> (0 until state.songs.size).random()
            else -> (state.currentIndex + 1) % state.songs.size
        }
        playSongAt(nextIndex)
    }

    fun playPrevious() {
        val state = _uiState.value
        val prevIndex = if (state.currentIndex == 0) state.songs.size - 1 else state.currentIndex - 1
        playSongAt(prevIndex)
    }

    fun cyclePlayMode() {
        val state = _uiState.value
        val next = when (state.playMode) {
            PlayMode.SEQUENCE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SEQUENCE
        }
        _uiState.value = state.copy(playMode = next)
    }

    fun seekTo(position: Long) {
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    fun toggleFullScreenPlayer() {
        _uiState.value = _uiState.value.copy(isFullScreenPlayer = !_uiState.value.isFullScreenPlayer)
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

    private fun startProgressSimulation() {
        progressJob = viewModelScope.launch {
            while (_uiState.value.playState == PlayState.PLAYING) {
                delay(500)
                val state = _uiState.value
                val newPos = state.currentPosition + 500
                if (newPos >= state.currentSong.duration) {
                    playNext()
                } else {
                    _uiState.value = state.copy(currentPosition = newPos)
                }
            }
        }
    }
}
