package com.livevault.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livevault.core.common.Result
import com.livevault.core.database.dao.NotificationDao
import com.livevault.core.database.entity.NotificationEntity
import com.livevault.core.network.dto.NotificationDto
import com.livevault.core.network.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val notifications: List<NotificationDto> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationDao: NotificationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) _uiState.update { it.copy(isRefreshing = true) }
            else _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val res = notificationRepository.getNotifications(page = 1, limit = 50)) {
                is Result.Success -> {
                    val list = res.data.notifications
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = list,
                            errorMessage = null
                        )
                    }
                    // Cache in Room
                    notificationDao.insertNotifications(list.map { dto ->
                        NotificationEntity(
                            id = dto.id,
                            type = dto.type,
                            title = dto.title,
                            message = dto.message,
                            recordingId = dto.recordingId,
                            channelId = dto.channelId,
                            isRead = dto.read,
                            createdAt = dto.createdAt
                        )
                    })
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = res.throwable.message
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markNotificationRead(id)
            notificationDao.markAsRead(id)
            _uiState.update { state ->
                state.copy(notifications = state.notifications.map {
                    if (it.id == id) it.copy(read = true) else it
                })
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllNotificationsRead()
            notificationDao.markAllAsRead()
            _uiState.update { state ->
                state.copy(notifications = state.notifications.map { it.copy(read = true) })
            }
        }
    }
}
