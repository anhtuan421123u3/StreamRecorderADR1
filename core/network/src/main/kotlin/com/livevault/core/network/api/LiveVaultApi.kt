package com.livevault.core.network.api

import com.livevault.core.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface LiveVaultApi {

    // ── Auth ─────────────────────────────────────────────────
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResultDto>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResultDto>>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<ApiResponse<TokenPairDto>>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/v1/auth/me")
    suspend fun me(): Response<ApiResponse<UserProfileDto>>

    @POST("api/v1/auth/apple")
    suspend fun appleSignIn(@Body request: AppleSignInRequest): Response<ApiResponse<AuthResultDto>>

    @POST("api/v1/auth/google")
    suspend fun googleSignIn(@Body request: GoogleSignInRequest): Response<ApiResponse<AuthResultDto>>

    // ── Channels ─────────────────────────────────────────────
    @GET("api/v1/channels")
    suspend fun getChannels(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<ApiResponse<ChannelListDto>>

    @POST("api/v1/channels")
    suspend fun addChannel(@Body request: AddChannelRequest): Response<ApiResponse<ChannelDto>>

    @GET("api/v1/channels/{id}")
    suspend fun getChannel(@Path("id") id: String): Response<ApiResponse<ChannelDto>>

    @PATCH("api/v1/channels/{id}")
    suspend fun updateChannel(
        @Path("id") id: String,
        @Body request: UpdateChannelRequest,
    ): Response<ApiResponse<ChannelDto>>

    @DELETE("api/v1/channels/{id}")
    suspend fun deleteChannel(@Path("id") id: String): Response<Unit>

    @POST("api/v1/channels/{id}/check-live")
    suspend fun checkLiveStatus(@Path("id") id: String): Response<ApiResponse<LiveStatusDto>>

    // ── Recordings ───────────────────────────────────────────
    @GET("api/v1/recordings")
    suspend fun getRecordings(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("channelId") channelId: String? = null,
        @Query("status") status: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Response<ApiResponse<RecordingListDto>>

    @GET("api/v1/recordings/storage-usage")
    suspend fun getStorageUsage(): Response<ApiResponse<StorageUsageDto>>

    @GET("api/v1/recordings/{id}")
    suspend fun getRecording(@Path("id") id: String): Response<ApiResponse<RecordingDetailDto>>

    @GET("api/v1/recordings/{id}/download")
    suspend fun getDownloadUrl(@Path("id") id: String): Response<ApiResponse<DownloadUrlDto>>

    @PATCH("api/v1/recordings/{id}")
    suspend fun updateRecording(
        @Path("id") id: String,
        @Body request: UpdateRecordingRequest,
    ): Response<ApiResponse<RecordingDetailDto>>

    @DELETE("api/v1/recordings/{id}")
    suspend fun deleteRecording(@Path("id") id: String): Response<Unit>

    // ── Subscription ─────────────────────────────────────────
    @GET("api/v1/subscription/status")
    suspend fun getSubscriptionStatus(): Response<ApiResponse<SubscriptionStatusDto>>

    @POST("api/v1/subscription/link-revenuecat")
    suspend fun linkRevenueCat(@Body request: LinkRevenueCatRequest): Response<Unit>

    // ── Notifications ────────────────────────────────────────
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("unreadOnly") unreadOnly: Boolean = false,
    ): Response<ApiResponse<NotificationListDto>>

    @PATCH("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<ApiResponse<Any>>

    @POST("api/v1/notifications/mark-all-read")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<Any>>

    // ── Devices ──────────────────────────────────────────────
    @PUT("api/v1/devices/token")
    suspend fun registerDeviceToken(@Body request: RegisterTokenRequest): Response<Unit>
}
