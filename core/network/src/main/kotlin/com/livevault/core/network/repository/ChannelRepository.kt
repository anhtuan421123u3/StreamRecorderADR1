package com.livevault.core.network.repository

import com.livevault.core.common.Result
import com.livevault.core.network.api.LiveVaultApi
import com.livevault.core.network.dto.*
import com.livevault.core.network.safeNetworkCall
import com.livevault.core.network.safeUnitCall
import javax.inject.Inject
import javax.inject.Singleton

interface ChannelRepository {
    suspend fun getChannels(page: Int = 1, limit: Int = 50): Result<ChannelListDto>
    suspend fun addChannel(request: AddChannelRequest): Result<ChannelDto>
    suspend fun getChannel(id: String): Result<ChannelDto>
    suspend fun updateChannel(id: String, request: UpdateChannelRequest): Result<ChannelDto>
    suspend fun deleteChannel(id: String): Result<Unit>
    suspend fun checkLiveStatus(id: String): Result<LiveStatusDto>
}

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val api: LiveVaultApi
) : ChannelRepository {

    override suspend fun getChannels(page: Int, limit: Int): Result<ChannelListDto> {
        return safeNetworkCall { api.getChannels(page, limit) }
    }

    override suspend fun addChannel(request: AddChannelRequest): Result<ChannelDto> {
        return safeNetworkCall { api.addChannel(request) }
    }

    override suspend fun getChannel(id: String): Result<ChannelDto> {
        return safeNetworkCall { api.getChannel(id) }
    }

    override suspend fun updateChannel(id: String, request: UpdateChannelRequest): Result<ChannelDto> {
        return safeNetworkCall { api.updateChannel(id, request) }
    }

    override suspend fun deleteChannel(id: String): Result<Unit> {
        return safeUnitCall { api.deleteChannel(id) }
    }

    override suspend fun checkLiveStatus(id: String): Result<LiveStatusDto> {
        return safeNetworkCall { api.checkLiveStatus(id) }
    }
}
