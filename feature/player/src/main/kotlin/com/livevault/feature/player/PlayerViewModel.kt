package com.livevault.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livevault.core.common.Result
import com.livevault.core.database.dao.DownloadDao
import com.livevault.core.database.dao.RecordingDao
import com.livevault.core.network.dto.RecordingDetailDto
import com.livevault.core.network.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class PlayerUiState(
    val isLoading: Boolean = true,
    val recording: RecordingDetailDto? = null,
    val playbackMediaUri: String? = null,
    val isLocalFile: Boolean = false,
    val currentPositionMs: Long = 0L,
    val isUrlRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val recordingDao: RecordingDao,
    private val downloadDao: DownloadDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recordingId: String = checkNotNull(savedStateHandle["recordingId"])

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPlaybackSource()
    }

    fun loadPlaybackSource() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Check if local download exists
            val localDownload = downloadDao.getDownloadById(recordingId)
            if (localDownload != null && localDownload.localPath.isNotBlank() && File(localDownload.localPath).exists()) {
                // Play local file directly
                val cachedRec = recordingDao.getRecordingById(recordingId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        playbackMediaUri = localDownload.localPath,
                        isLocalFile = true,
                        recording = cachedRec?.let { c ->
                            RecordingDetailDto(
                                id = c.id,
                                channelId = c.channelId,
                                channelName = c.channelName,
                                title = c.title,
                                durationSeconds = c.durationSeconds,
                                sizeBytes = c.sizeBytes,
                                status = c.status,
                                recordedAt = c.recordedAt,
                                playbackUrl = localDownload.localPath,
                                thumbnailUrl = c.thumbnailUrl
                            )
                        }
                    )
                }
                return@launch
            }

            // 2. Fetch remote signed URL from backend
            fetchRemotePlaybackUrl()
        }
    }

    private suspend fun fetchRemotePlaybackUrl(resumePositionMs: Long = 0L) {
        when (val res = recordingRepository.getRecording(recordingId)) {
            is Result.Success -> {
                val detail = res.data
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isUrlRefreshing = false,
                        recording = detail,
                        playbackMediaUri = detail.playbackUrl,
                        isLocalFile = false,
                        currentPositionMs = resumePositionMs
                    )
                }
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isUrlRefreshing = false,
                        errorMessage = res.throwable.message ?: "Failed to load stream"
                    )
                }
            }
            is Result.Loading -> {}
        }
    }

    /**
     * Auto-retry logic: Called when ExoPlayer encounters an error (HTTP 403 / signed URL expired after 15m)
     */
    fun onPlaybackError(currentPos: Long) {
        if (_uiState.value.isLocalFile) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUrlRefreshing = true) }
            fetchRemotePlaybackUrl(resumePositionMs = currentPos)
        }
    }

    fun updatePosition(pos: Long) {
        _uiState.update { it.copy(currentPositionMs = pos) }
    }
}
