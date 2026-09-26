package com.livevault.core.network

import com.google.gson.Gson
import com.livevault.core.common.Result
import com.livevault.core.network.api.LiveVaultApi
import com.livevault.core.network.repository.RecordingRepository
import com.livevault.core.network.repository.RecordingRepositoryImpl
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecordingRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: RecordingRepository

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(LiveVaultApi::class.java)

        repository = RecordingRepositoryImpl(api)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getDownloadUrl returns signed download url on HTTP 200`() = runBlocking {
        val jsonResponse = """
            {
                "success": true,
                "data": {
                    "downloadUrl": "https://r2.livevault.app/download/rec_1.mp4?token=abc",
                    "expiresIn": 3600
                }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json")
        )

        val result = repository.getDownloadUrl("rec_1")
        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals("https://r2.livevault.app/download/rec_1.mp4?token=abc", data.downloadUrl)
        assertEquals(3600, data.expiresIn)
    }

    @Test
    fun `getRecording returns error on HTTP 404`() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"success": false, "error": "Recording not found"}""")
        )

        val result = repository.getRecording("invalid_id")
        assertTrue(result is Result.Error)
    }
}
