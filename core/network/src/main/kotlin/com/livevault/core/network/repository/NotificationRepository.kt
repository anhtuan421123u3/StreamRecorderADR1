package com.livevault.core.network.repository

import com.livevault.core.common.Result
import com.livevault.core.network.api.LiveVaultApi
import com.livevault.core.network.dto.NotificationListDto
import com.livevault.core.network.dto.RegisterTokenRequest
import com.livevault.core.network.safeNetworkCall
import com.livevault.core.network.safeUnitCall
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationRepository {
    suspend fun getNotifications(page: Int = 1, limit: Int = 20, unreadOnly: Boolean = false): Result<NotificationListDto>
    suspend fun markNotificationRead(id: String): Result<Unit>
    suspend fun markAllNotificationsRead(): Result<Unit>
    suspend fun registerDeviceToken(token: String, platform: String = "ANDROID"): Result<Unit>
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val api: LiveVaultApi
) : NotificationRepository {

    override suspend fun getNotifications(
        page: Int,
        limit: Int,
        unreadOnly: Boolean
    ): Result<NotificationListDto> {
        return safeNetworkCall { api.getNotifications(page, limit, unreadOnly) }
    }

    override suspend fun markNotificationRead(id: String): Result<Unit> {
        return when (val res = safeNetworkCall { api.markNotificationRead(id) }) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun markAllNotificationsRead(): Result<Unit> {
        return when (val res = safeNetworkCall { api.markAllNotificationsRead() }) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> res
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun registerDeviceToken(token: String, platform: String): Result<Unit> {
        return safeUnitCall { api.registerDeviceToken(RegisterTokenRequest(token, platform)) }
    }
}
