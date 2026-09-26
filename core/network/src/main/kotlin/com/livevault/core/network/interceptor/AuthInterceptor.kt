package com.livevault.core.network.interceptor

import com.livevault.core.network.dto.RefreshRequest
import com.livevault.core.network.token.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Provider

/**
 * Attaches Bearer token to every request.
 * On HTTP 401 → automatically refreshes the access token and retries once.
 *
 * Uses Provider<Retrofit> (lazy) to avoid circular Hilt dependency.
 */
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val retrofitProvider: Provider<Retrofit>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = runBlocking { tokenStore.getAccessToken() }
        val originalRequest = chain.request()

        val request = originalRequest.withBearer(accessToken)
        val response = chain.proceed(request)

        return if (response.code == 401) {
            response.close()
            val refreshed = runBlocking { refreshToken() }
            if (refreshed != null) {
                chain.proceed(originalRequest.withBearer(refreshed))
            } else {
                // Refresh failed — clear tokens so app navigates to login
                runBlocking { tokenStore.clearTokens() }
                response
            }
        } else {
            response
        }
    }

    private suspend fun refreshToken(): String? {
        val refreshToken = tokenStore.getRefreshToken() ?: return null
        return try {
            val retrofit = retrofitProvider.get()
            val api = retrofit.create(com.livevault.core.network.api.LiveVaultApi::class.java)
            val response = api.refresh(RefreshRequest(refreshToken))
            if (response.isSuccessful) {
                val tokens = response.body()?.data
                if (tokens != null) {
                    tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken)
                    tokens.accessToken
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun Request.withBearer(token: String?): Request =
        if (token != null) newBuilder().header("Authorization", "Bearer $token").build()
        else this
}
