package com.livevault.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline & Local Mock Interceptor:
 * Cho phép chạy và test đầy đủ tính năng của ứng dụng mà không cần tốn tiền mua domain hay thuê server cloud.
 *
 * - Nếu URL trỏ vào domain chưa đăng ký ("livevault.app"): Trả về mock data ngay lập tức.
 * - Nếu URL trỏ vào localhost/LAN IP nhưng backend chưa bật: Tự động fallback về mock data.
 * - Nếu backend local đang bật: Chuyển tiếp request bình thường tới backend.
 */
@Singleton
class MockInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val path = url.encodedPath

        // Nếu đang dùng domain giả định livevault.app -> trả về mock ngay, không gọi DNS tránh lỗi UnknownHostException
        if (url.host.contains("livevault.app")) {
            return buildMockResponse(request, path)
        }

        // Với local IP (10.0.2.2 / 192.168.x.x / localhost): Thử gọi backend trước
        return try {
            val response = chain.proceed(request)
            if (response.isSuccessful || response.code < 500) {
                response
            } else {
                buildMockResponse(request, path)
            }
        } catch (e: IOException) {
            // Backend offline hoặc không kết nối được -> fallback sang mock
            buildMockResponse(request, path)
        }
    }

    private fun buildMockResponse(request: Request, path: String): Response {
        val method = request.method
        val jsonString = when {
            // Auth endpoints
            path.endsWith("/auth/register") || path.endsWith("/auth/login") ||
            path.endsWith("/auth/google") || path.endsWith("/auth/apple") -> {
                var email = "tuanpath06170@gmail.com"
                var displayName = "tuananh1310"
                try {
                    val buffer = Buffer()
                    request.body?.writeTo(buffer)
                    val bodyStr = buffer.readUtf8()
                    val reqJson = JSONObject(bodyStr)
                    if (reqJson.has("email")) email = reqJson.optString("email", email)
                    if (reqJson.has("displayName") && !reqJson.isNull("displayName")) {
                        displayName = reqJson.optString("displayName", displayName)
                    }
                } catch (_: Exception) {}

                """
                {
                  "success": true,
                  "data": {
                    "user": {
                      "id": "usr_demo_1310",
                      "email": "$email",
                      "displayName": "$displayName",
                      "avatarUrl": "https://picsum.photos/200",
                      "subscriptionTier": "PRO",
                      "subscriptionExpiry": null
                    },
                    "tokens": {
                      "accessToken": "mock_access_token_${System.currentTimeMillis()}",
                      "refreshToken": "mock_refresh_token_${System.currentTimeMillis()}",
                      "expiresIn": 86400
                    }
                  },
                  "message": "Success"
                }
                """.trimIndent()
            }

            path.endsWith("/auth/refresh") -> {
                """
                {
                  "success": true,
                  "data": {
                    "accessToken": "mock_refreshed_access_token_${System.currentTimeMillis()}",
                    "refreshToken": "mock_refreshed_refresh_token_${System.currentTimeMillis()}",
                    "expiresIn": 86400
                  }
                }
                """.trimIndent()
            }

            path.endsWith("/auth/me") -> {
                """
                {
                  "success": true,
                  "data": {
                    "id": "usr_demo_1310",
                    "email": "tuanpath06170@gmail.com",
                    "displayName": "tuananh1310",
                    "avatarUrl": "https://picsum.photos/200",
                    "subscriptionTier": "PRO",
                    "subscriptionExpiry": null
                  }
                }
                """.trimIndent()
            }

            path.endsWith("/auth/logout") -> {
                """{ "success": true, "data": null }"""
            }

            // Subscription status
            path.endsWith("/subscription/status") -> {
                """
                {
                  "success": true,
                  "data": {
                    "tier": "PRO",
                    "expiresAt": "2030-01-01T00:00:00.000Z",
                    "isActive": true
                  }
                }
                """.trimIndent()
            }

            // Channels
            path.endsWith("/channels") && method == "GET" -> {
                """
                {
                  "success": true,
                  "data": {
                    "channels": [
                      {
                        "id": "ch_tiktok_1",
                        "platform": "TIKTOK",
                        "channelId": "live_gaming_vn",
                        "channelUrl": "https://www.tiktok.com/@live_gaming_vn/live",
                        "displayName": "TikTok Gaming VN",
                        "avatarUrl": "https://picsum.photos/201",
                        "isActive": true,
                        "isLive": true,
                        "lastCheckedAt": "2026-09-25T15:00:00.000Z",
                        "streamTitle": "Live Stream Leo Rank Demacia"
                      },
                      {
                        "id": "ch_fb_2",
                        "platform": "FACEBOOK",
                        "channelId": "fb_streamer_pro",
                        "channelUrl": "https://www.facebook.com/gaming/live",
                        "displayName": "Facebook Gaming VN",
                        "avatarUrl": "https://picsum.photos/202",
                        "isActive": true,
                        "isLive": false,
                        "lastCheckedAt": "2026-09-25T14:30:00.000Z",
                        "streamTitle": "Offline"
                      }
                    ],
                    "total": 2,
                    "page": 1,
                    "limit": 50
                  }
                }
                """.trimIndent()
            }

            path.endsWith("/channels") && method == "POST" -> {
                var platform = "TIKTOK"
                var channelUrl = "https://www.tiktok.com/@streamer/live"
                var displayName = "Kênh Mới"
                try {
                    val buffer = Buffer()
                    request.body?.writeTo(buffer)
                    val bodyStr = buffer.readUtf8()
                    val reqJson = JSONObject(bodyStr)
                    if (reqJson.has("platform")) platform = reqJson.optString("platform", platform)
                    if (reqJson.has("channelUrl")) channelUrl = reqJson.optString("channelUrl", channelUrl)
                    if (reqJson.has("displayName") && !reqJson.isNull("displayName")) {
                        displayName = reqJson.optString("displayName", displayName)
                    }
                } catch (_: Exception) {}

                """
                {
                  "success": true,
                  "data": {
                    "id": "ch_${System.currentTimeMillis()}",
                    "platform": "$platform",
                    "channelId": "chan_${System.currentTimeMillis()}",
                    "channelUrl": "$channelUrl",
                    "displayName": "$displayName",
                    "avatarUrl": "https://picsum.photos/203",
                    "isActive": true,
                    "isLive": true,
                    "lastCheckedAt": "2026-09-25T15:00:00.000Z",
                    "streamTitle": "Livestream đang chạy"
                  }
                }
                """.trimIndent()
            }

            path.contains("/channels/") && path.endsWith("/check-live") -> {
                """
                {
                  "success": true,
                  "data": {
                    "channelId": "ch_tiktok_1",
                    "isLive": true,
                    "lastCheckedAt": "2026-09-25T15:00:00.000Z",
                    "streamTitle": "Livestream trực tiếp"
                  }
                }
                """.trimIndent()
            }

            path.contains("/channels/") && method == "GET" -> {
                """
                {
                  "success": true,
                  "data": {
                    "id": "ch_tiktok_1",
                    "platform": "TIKTOK",
                    "channelId": "live_gaming_vn",
                    "channelUrl": "https://www.tiktok.com/@live_gaming_vn/live",
                    "displayName": "TikTok Gaming VN",
                    "avatarUrl": "https://picsum.photos/201",
                    "isActive": true,
                    "isLive": true,
                    "lastCheckedAt": "2026-09-25T15:00:00.000Z",
                    "streamTitle": "Live Stream Leo Rank Demacia"
                  }
                }
                """.trimIndent()
            }

            // Recordings
            path.endsWith("/recordings") && method == "GET" -> {
                """
                {
                  "success": true,
                  "data": {
                    "recordings": [
                      {
                        "id": "rec_demo_1",
                        "channelId": "ch_tiktok_1",
                        "channelName": "TikTok Gaming VN",
                        "channelAvatar": "https://picsum.photos/201",
                        "title": "Livestream giải đấu kịch tính #1",
                        "durationSeconds": 1800,
                        "sizeBytes": 154857600,
                        "status": "COMPLETED",
                        "recordedAt": "2026-09-25T12:00:00.000Z",
                        "thumbnailUrl": "https://picsum.photos/seed/rec1/640/360",
                        "hlsManifestKey": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                      },
                      {
                        "id": "rec_demo_2",
                        "channelId": "ch_tiktok_1",
                        "channelName": "TikTok Gaming VN",
                        "channelAvatar": "https://picsum.photos/201",
                        "title": "Highlight pha combat ấn tượng",
                        "durationSeconds": 900,
                        "sizeBytes": 84288000,
                        "status": "COMPLETED",
                        "recordedAt": "2026-09-24T18:00:00.000Z",
                        "thumbnailUrl": "https://picsum.photos/seed/rec2/640/360",
                        "hlsManifestKey": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
                      }
                    ],
                    "total": 2,
                    "page": 1,
                    "limit": 20
                  }
                }
                """.trimIndent()
            }

            path.contains("/recordings/") && path.endsWith("/playback-url") -> {
                """
                {
                  "success": true,
                  "data": {
                    "playbackUrl": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                  }
                }
                """.trimIndent()
            }

            path.contains("/recordings/") && path.endsWith("/download-url") -> {
                """
                {
                  "success": true,
                  "data": {
                    "downloadUrl": "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "expiresIn": 3600
                  }
                }
                """.trimIndent()
            }

            path.endsWith("/recordings/storage/usage") -> {
                """
                {
                  "success": true,
                  "data": {
                    "usedBytes": 239145600,
                    "maxBytes": 10737418240,
                    "tier": "PRO"
                  }
                }
                """.trimIndent()
            }

            // Notifications
            path.endsWith("/notifications") -> {
                """
                {
                  "success": true,
                  "data": {
                    "notifications": [
                      {
                        "id": "notif_1",
                        "type": "RECORDING_FINISHED",
                        "title": "Bản ghi hoàn tất",
                        "message": "Kênh TikTok Gaming VN đã kết thúc live. Bạn có thể xem ngay!",
                        "recordingId": "rec_demo_1",
                        "channelId": "ch_tiktok_1",
                        "read": false,
                        "createdAt": "2026-09-25T13:00:00.000Z"
                      }
                    ],
                    "total": 1,
                    "page": 1,
                    "limit": 20
                  }
                }
                """.trimIndent()
            }

            // Devices / FCM
            path.endsWith("/devices") -> {
                """{ "success": true, "data": null }"""
            }

            // Default fallback
            else -> {
                """{ "success": true, "data": {} }"""
            }
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK (Mock Data)")
            .body(jsonString.toResponseBody("application/json".toMediaTypeOrNull()))
            .build()
    }
}
