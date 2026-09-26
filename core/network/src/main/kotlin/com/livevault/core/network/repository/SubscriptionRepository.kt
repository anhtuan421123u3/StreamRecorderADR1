package com.livevault.core.network.repository

import com.livevault.core.common.Result
import com.livevault.core.network.api.LiveVaultApi
import com.livevault.core.network.dto.LinkRevenueCatRequest
import com.livevault.core.network.dto.SubscriptionStatusDto
import com.livevault.core.network.safeNetworkCall
import com.livevault.core.network.safeUnitCall
import javax.inject.Inject
import javax.inject.Singleton

interface SubscriptionRepository {
    suspend fun getSubscriptionStatus(): Result<SubscriptionStatusDto>
    suspend fun linkRevenueCat(appUserId: String): Result<Unit>
}

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val api: LiveVaultApi
) : SubscriptionRepository {

    override suspend fun getSubscriptionStatus(): Result<SubscriptionStatusDto> {
        return safeNetworkCall { api.getSubscriptionStatus() }
    }

    override suspend fun linkRevenueCat(appUserId: String): Result<Unit> {
        return safeUnitCall { api.linkRevenueCat(LinkRevenueCatRequest(appUserId)) }
    }
}
