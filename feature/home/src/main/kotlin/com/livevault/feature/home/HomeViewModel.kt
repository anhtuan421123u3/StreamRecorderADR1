package com.livevault.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livevault.core.common.Result
import com.livevault.core.database.dao.ChannelDao
import com.livevault.core.database.dao.RecordingDao
import com.livevault.core.database.entity.ChannelEntity
import com.livevault.core.database.entity.RecordingEntity
import com.livevault.core.network.dto.ChannelDto
import com.livevault.core.network.dto.RecordingItemDto
import com.livevault.core.network.repository.ChannelRepository
import com.livevault.core.network.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val channels: List<ChannelDto> = emptyList(),
    val liveChannels: List<ChannelDto> = emptyList(),
    val recentRecordings: List<RecordingItemDto> = emptyList(),
    val activeRecordingsCount: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val recordingRepository: RecordingRepository,
    private val channelDao: ChannelDao,
    private val recordingDao: RecordingDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        loadData(isRefresh = true)
    }

    fun loadData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val channelsDeferred = async { channelRepository.getChannels(page = 1, limit = 50) }
            val recordingsDeferred = async { recordingRepository.getRecordings(page = 1, limit = 20) }

            val channelsResult = channelsDeferred.await()
            val recordingsResult = recordingsDeferred.await()

            var newChannels = _uiState.value.channels
            var newRecentRecordings = _uiState.value.recentRecordings
            var errorMsg: String? = null

            when (channelsResult) {
                is Result.Success -> {
                    newChannels = channelsResult.data.channels
                    // Cache in Room
                    channelDao.insertChannels(newChannels.map { dto ->
                        ChannelEntity(
                            id = dto.id,
                            platform = dto.platform,
                            channelId = dto.channelId,
                            channelUrl = dto.channelUrl,
                            displayName = dto.displayName,
                            avatarUrl = dto.avatarUrl,
                            isActive = dto.isActive,
                            isLive = dto.isLive,
                            lastCheckedAt = dto.lastCheckedAt,
                            streamTitle = dto.streamTitle
                        )
                    })
                }
                is Result.Error -> {
                    errorMsg = channelsResult.throwable.message
                }
                is Result.Loading -> {}
            }

            when (recordingsResult) {
                is Result.Success -> {
                    newRecentRecordings = recordingsResult.data.recordings
                    // Cache in Room
                    recordingDao.insertRecordings(newRecentRecordings.map { dto ->
                        RecordingEntity(
                            id = dto.id,
                            channelId = dto.channelId,
                            channelName = dto.channelName,
                            channelAvatar = dto.channelAvatar,
                            title = dto.title,
                            durationSeconds = dto.durationSeconds,
                            sizeBytes = dto.sizeBytes,
                            status = dto.status,
                            recordedAt = dto.recordedAt,
                            thumbnailUrl = dto.thumbnailUrl,
                            hlsManifestKey = dto.hlsManifestKey
                        )
                    })
                }
                is Result.Error -> {
                    if (errorMsg == null) errorMsg = recordingsResult.throwable.message
                }
                is Result.Loading -> {}
            }

            val live = newChannels.filter { it.isLive }
            val activeRecCount = newRecentRecordings.count { it.status == "RECORDING" }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    channels = newChannels,
                    liveChannels = live,
                    recentRecordings = newRecentRecordings,
                    activeRecordingsCount = activeRecCount,
                    errorMessage = errorMsg
                )
            }
        }
    }

    fun checkLiveStatus(channelId: String) {
        viewModelScope.launch {
            when (val res = channelRepository.checkLiveStatus(channelId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        val updated = state.channels.map { ch ->
                            if (ch.id == channelId) {
                                ch.copy(
                                    isLive = res.data.isLive,
                                    streamTitle = res.data.streamTitle,
                                    lastCheckedAt = res.data.lastCheckedAt
                                )
                            } else ch
                        }
                        state.copy(
                            channels = updated,
                            liveChannels = updated.filter { it.isLive }
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
