package com.livevault.core.network.dto

import com.google.gson.annotations.SerializedName

// ── Common ────────────────────────────────────────────────────
data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T,
    val message: String? = null,
    val error: String? = null
)

// ── Auth ──────────────────────────────────────────────────────
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class AppleUserDto(
    val name: String? = null,
    val email: String? = null
)

data class AppleSignInRequest(
    val identityToken: String,
    val user: AppleUserDto? = null
)

data class GoogleSignInRequest(
    val idToken: String
)

data class TokenPairDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int = 900
)

data class AuthResultDto(
    val user: UserProfileDto,
    val tokens: TokenPairDto
)

data class UserProfileDto(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val subscriptionTier: String = "FREE",
    val subscriptionExpiry: String? = null
)

// ── Channels ──────────────────────────────────────────────────
data class ChannelDto(
    val id: String,
    val platform: String,
    val channelId: String = "",
    val channelUrl: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isActive: Boolean = true,
    val isLive: Boolean = false,
    val lastCheckedAt: String? = null,
    val streamTitle: String? = null
)

data class ChannelConsentDto(
    val hasPermission: Boolean = true,
    val consentText: String
)

data class AddChannelRequest(
    val platform: String,
    val channelUrl: String,
    val displayName: String? = null,
    val streamKey: String? = null,
    val consent: ChannelConsentDto
)

data class UpdateChannelRequest(
    val displayName: String? = null,
    val isActive: Boolean? = null
)

data class ChannelListDto(
    val channels: List<ChannelDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 50
)

data class LiveStatusDto(
    val channelId: String,
    val isLive: Boolean,
    val lastCheckedAt: String? = null,
    val streamTitle: String? = null
)

// ── Recordings ────────────────────────────────────────────────
data class RecordingItemDto(
    val id: String,
    val channelId: String,
    val channelName: String,
    val channelAvatar: String? = null,
    val title: String,
    val durationSeconds: Long = 0L,
    val sizeBytes: Long = 0L,
    val status: String,
    val recordedAt: String,
    val thumbnailUrl: String? = null,
    val hlsManifestKey: String? = null
)

typealias RecordingDto = RecordingItemDto

data class RecordingDetailDto(
    val id: String,
    val channelId: String,
    val channelName: String,
    val title: String,
    val durationSeconds: Long = 0L,
    val sizeBytes: Long = 0L,
    val status: String,
    val recordedAt: String,
    val playbackUrl: String? = null,
    val thumbnailUrl: String? = null
)

data class RecordingListDto(
    val recordings: List<RecordingItemDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

data class UpdateRecordingRequest(
    val title: String? = null,
    val notes: String? = null
)

data class DownloadUrlDto(
    val downloadUrl: String,
    val expiresIn: Int = 3600
)

data class StorageUsageDto(
    val usedBytes: Long = 0L,
    val maxBytes: Long = 0L,
    val tier: String = "FREE"
)

// ── Subscription ──────────────────────────────────────────────
data class SubscriptionStatusDto(
    val tier: String = "FREE",
    val expiresAt: String? = null,
    val isActive: Boolean = true
)

data class LinkRevenueCatRequest(
    val appUserId: String
)

// ── Notifications ─────────────────────────────────────────────
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val recordingId: String? = null,
    val channelId: String? = null,
    val read: Boolean = false,
    val createdAt: String
)

data class NotificationListDto(
    val notifications: List<NotificationDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

// ── Devices ───────────────────────────────────────────────────
data class RegisterTokenRequest(
    val token: String,
    val platform: String = "ANDROID"
)
