package com.livevault.feature.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livevault.core.common.Result
import com.livevault.core.database.dao.ChannelDao
import com.livevault.core.database.entity.ChannelEntity
import com.livevault.core.network.dto.AddChannelRequest
import com.livevault.core.network.dto.ChannelConsentDto
import com.livevault.core.network.dto.ChannelDto
import com.livevault.core.network.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val selectedPlatform: String = "YOUTUBE",
    val channelUrl: String = "",
    val displayName: String = "",
    val customStreamKey: String = "",
    val consentAgreed: Boolean = false,
    val errorMessage: String? = null,
    val currentChannel: ChannelDto? = null
)

sealed interface ChannelEvent {
    object ChannelAddedSuccessfully : ChannelEvent
    object ChannelDeletedSuccessfully : ChannelEvent
    data class ShowToast(val message: String) : ChannelEvent
}

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val channelDao: ChannelDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ChannelEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onPlatformSelected(platform: String) {
        _uiState.update { it.copy(selectedPlatform = platform) }
    }

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(channelUrl = url, errorMessage = null) }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    fun onStreamKeyChanged(key: String) {
        _uiState.update { it.copy(customStreamKey = key) }
    }

    fun onConsentToggled(agreed: Boolean) {
        _uiState.update { it.copy(consentAgreed = agreed) }
    }

    fun addChannel() {
        val state = _uiState.value
        if (state.channelUrl.isBlank() && state.selectedPlatform != "CUSTOM_RTMP") {
            _uiState.update { it.copy(errorMessage = "Please provide the channel URL") }
            return
        }
        if (!state.consentAgreed) {
            _uiState.update { it.copy(errorMessage = "You must confirm you own this channel or have explicit permission to record it.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val consent = ChannelConsentDto(
                hasPermission = true,
                consentText = "I confirm under penalty of law that I own this channel or have obtained explicit legal authorization from the content owner to record all live broadcasts."
            )

            val request = AddChannelRequest(
                platform = state.selectedPlatform,
                channelUrl = state.channelUrl.trim(),
                displayName = state.displayName.ifBlank { null },
                streamKey = if (state.selectedPlatform == "CUSTOM_RTMP") state.customStreamKey.trim() else null,
                consent = consent
            )

            when (val res = channelRepository.addChannel(request)) {
                is Result.Success -> {
                    val dto = res.data
                    channelDao.insertChannel(
                        ChannelEntity(
                            id = dto.id,
                            platform = dto.platform,
                            channelId = dto.channelId,
                            channelUrl = dto.channelUrl,
                            displayName = dto.displayName,
                            avatarUrl = dto.avatarUrl,
                            isActive = dto.isActive,
                            isLive = dto.isLive,
                            lastCheckedAt = dto.lastCheckedAt
                        )
                    )
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    _eventFlow.emit(ChannelEvent.ChannelAddedSuccessfully)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = res.throwable.message ?: "Failed to add channel"
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun loadChannelDetail(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = channelRepository.getChannel(id)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, currentChannel = res.data) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.throwable.message) }
                is Result.Loading -> {}
            }
        }
    }

    fun deleteChannel(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (channelRepository.deleteChannel(id)) {
                is Result.Success -> {
                    channelDao.deleteChannelById(id)
                    _uiState.update { it.copy(isLoading = false) }
                    _eventFlow.emit(ChannelEvent.ChannelDeletedSuccessfully)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to delete channel") }
                }
                is Result.Loading -> {}
            }
        }
    }
}
