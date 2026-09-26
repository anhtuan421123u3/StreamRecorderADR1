package com.livevault.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livevault.core.common.Result
import com.livevault.core.database.dao.ChannelDao
import com.livevault.core.database.dao.DownloadDao
import com.livevault.core.database.dao.NotificationDao
import com.livevault.core.database.dao.RecordingDao
import com.livevault.core.network.dto.StorageUsageDto
import com.livevault.core.network.dto.SubscriptionStatusDto
import com.livevault.core.network.dto.UserProfileDto
import com.livevault.core.network.repository.AuthRepository
import com.livevault.core.network.repository.RecordingRepository
import com.livevault.core.network.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val profile: UserProfileDto? = null,
    val subscription: SubscriptionStatusDto? = null,
    val storageUsage: StorageUsageDto? = null,
    val errorMessage: String? = null
)

sealed interface SettingsEvent {
    object LoggedOut : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val recordingRepository: RecordingRepository,
    private val channelDao: ChannelDao,
    private val recordingDao: RecordingDao,
    private val downloadDao: DownloadDao,
    private val notificationDao: NotificationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SettingsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        loadSettingsData()
    }

    fun loadSettingsData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val profileRes = authRepository.getProfile()
            val subRes = subscriptionRepository.getSubscriptionStatus()
            val storageRes = recordingRepository.getStorageUsage()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    profile = if (profileRes is Result.Success) profileRes.data else null,
                    subscription = if (subRes is Result.Success) subRes.data else null,
                    storageUsage = if (storageRes is Result.Success) storageRes.data else null
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            // Clear local database caches
            channelDao.clearAll()
            recordingDao.clearAll()
            notificationDao.clearAll()
            _eventFlow.emit(SettingsEvent.LoggedOut)
        }
    }
}
