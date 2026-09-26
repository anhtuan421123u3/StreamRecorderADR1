package com.livevault.core.network.repository

import com.livevault.core.common.Result
import com.livevault.core.network.api.LiveVaultApi
import com.livevault.core.network.dto.*
import com.livevault.core.network.safeNetworkCall
import com.livevault.core.network.safeUnitCall
import com.livevault.core.network.token.TokenStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val accessTokenFlow: Flow<String?>
    suspend fun register(request: RegisterRequest): Result<AuthResultDto>
    suspend fun login(request: LoginRequest): Result<AuthResultDto>
    suspend fun googleSignIn(idToken: String): Result<AuthResultDto>
    suspend fun appleSignIn(identityToken: String, user: AppleUserDto? = null): Result<AuthResultDto>
    suspend fun getProfile(): Result<UserProfileDto>
    suspend fun logout(): Result<Unit>
    suspend fun getAccessToken(): String?
    suspend fun clearTokens()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: LiveVaultApi,
    private val tokenStore: TokenStore,
) : AuthRepository {

    override val accessTokenFlow: Flow<String?> = tokenStore.accessTokenFlow()

    override suspend fun register(request: RegisterRequest): Result<AuthResultDto> {
        return when (val result = safeNetworkCall { api.register(request) }) {
            is Result.Success -> {
                val data = result.data
                tokenStore.saveTokens(data.tokens.accessToken, data.tokens.refreshToken)
                Result.Success(data)
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun login(request: LoginRequest): Result<AuthResultDto> {
        return when (val result = safeNetworkCall { api.login(request) }) {
            is Result.Success -> {
                val data = result.data
                tokenStore.saveTokens(data.tokens.accessToken, data.tokens.refreshToken)
                Result.Success(data)
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun googleSignIn(idToken: String): Result<AuthResultDto> {
        return when (val result = safeNetworkCall { api.googleSignIn(GoogleSignInRequest(idToken)) }) {
            is Result.Success -> {
                val data = result.data
                tokenStore.saveTokens(data.tokens.accessToken, data.tokens.refreshToken)
                Result.Success(data)
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun appleSignIn(identityToken: String, user: AppleUserDto?): Result<AuthResultDto> {
        return when (val result = safeNetworkCall { api.appleSignIn(AppleSignInRequest(identityToken, user)) }) {
            is Result.Success -> {
                val data = result.data
                tokenStore.saveTokens(data.tokens.accessToken, data.tokens.refreshToken)
                Result.Success(data)
            }
            is Result.Error -> result
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun getProfile(): Result<UserProfileDto> {
        return safeNetworkCall { api.me() }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            safeUnitCall { api.logout() }
            tokenStore.clearTokens()
            Result.Success(Unit)
        } catch (e: Exception) {
            tokenStore.clearTokens()
            Result.Error(e)
        }
    }

    override suspend fun getAccessToken(): String? = tokenStore.getAccessToken()

    override suspend fun clearTokens() = tokenStore.clearTokens()
}
