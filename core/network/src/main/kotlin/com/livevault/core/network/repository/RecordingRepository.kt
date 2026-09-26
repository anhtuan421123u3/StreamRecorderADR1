package com.livevault.core.network.repository

import com.livevault.core.common.Result
import com.livevault.core.network.api.LiveVaultApi
import com.livevault.core.network.dto.*
import com.livevault.core.network.safeNetworkCall
import com.livevault.core.network.safeUnitCall
import javax.inject.Inject
import javax.inject.Singleton

interface RecordingRepository {
    suspend fun getRecordings(
        page: Int = 1,
        limit: Int = 20,
        channelId: String? = null,
        status: String? = null,
        from: String? = null,
        to: String? = null
    ): Result<RecordingListDto>

    suspend fun getRecording(id: String): Result<RecordingDetailDto>
    suspend fun getDownloadUrl(id: String): Result<DownloadUrlDto>
    suspend fun updateRecording(id: String, request: UpdateRecordingRequest): Result<RecordingDetailDto>
    suspend fun deleteRecording(id: String): Result<Unit>
    suspend fun getStorageUsage(): Result<StorageUsageDto>
}

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val api: LiveVaultApi
) : RecordingRepository {

    override suspend fun getRecordings(
        page: Int,
        limit: Int,
        channelId: String?,
        status: String?,
        from: String?,
        to: String?
    ): Result<RecordingListDto> {
        return safeNetworkCall {
            api.getRecordings(page, limit, channelId, status, from, to)
        }
    }

    override suspend fun getRecording(id: String): Result<RecordingDetailDto> {
        return safeNetworkCall { api.getRecording(id) }
    }

    override suspend fun getDownloadUrl(id: String): Result<DownloadUrlDto> {
        return safeNetworkCall { api.getDownloadUrl(id) }
    }

    override suspend fun updateRecording(
        id: String,
        request: UpdateRecordingRequest
    ): Result<RecordingDetailDto> {
        return safeNetworkCall { api.updateRecording(id, request) }
    }

    override suspend fun deleteRecording(id: String): Result<Unit> {
        return safeUnitCall { api.deleteRecording(id) }
    }

    override suspend fun getStorageUsage(): Result<StorageUsageDto> {
        return safeNetworkCall { api.getStorageUsage() }
    }
}
