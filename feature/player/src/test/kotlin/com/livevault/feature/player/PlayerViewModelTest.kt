package com.livevault.feature.player

import androidx.lifecycle.SavedStateHandle
import com.livevault.core.common.Result
import com.livevault.core.database.dao.DownloadDao
import com.livevault.core.database.dao.RecordingDao
import com.livevault.core.network.dto.RecordingDetailDto
import com.livevault.core.network.repository.RecordingRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val recordingRepository: RecordingRepository = mockk(relaxed = true)
    private val recordingDao: RecordingDao = mockk(relaxed = true)
    private val downloadDao: DownloadDao = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPlaybackSource fetches remote signed URL when local download does not exist`() = runTest {
        val dummyDetail = RecordingDetailDto(
            id = "rec_123",
            channelId = "ch_1",
            channelName = "Gamer",
            title = "Apex Live",
            durationSeconds = 1800L,
            sizeBytes = 500000000L,
            status = "READY",
            recordedAt = "2026-09-22T08:00:00Z",
            playbackUrl = "https://r2.livevault.app/playback/rec_123.m3u8?token=xyz",
            thumbnailUrl = null
        )

        coEvery { downloadDao.getDownloadById("rec_123") } returns null
        coEvery { recordingRepository.getRecording("rec_123") } returns Result.Success(dummyDetail)

        val savedStateHandle = SavedStateHandle(mapOf("recordingId" to "rec_123"))
        val viewModel = PlayerViewModel(
            recordingRepository,
            recordingDao,
            downloadDao,
            savedStateHandle
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isLocalFile)
        assertEquals("https://r2.livevault.app/playback/rec_123.m3u8?token=xyz", state.playbackMediaUri)
        assertEquals("Apex Live", state.recording?.title)
    }

    @Test
    fun `onPlaybackError refreshes remote signed URL while preserving current playback position`() = runTest {
        val initialDetail = RecordingDetailDto(
            id = "rec_123",
            channelId = "ch_1",
            channelName = "Gamer",
            title = "Apex Live",
            durationSeconds = 1800L,
            sizeBytes = 500000000L,
            status = "READY",
            recordedAt = "2026-09-22T08:00:00Z",
            playbackUrl = "https://r2.livevault.app/playback/rec_123.m3u8?token=old",
            thumbnailUrl = null
        )

        val refreshedDetail = initialDetail.copy(
            playbackUrl = "https://r2.livevault.app/playback/rec_123.m3u8?token=fresh_new"
        )

        coEvery { downloadDao.getDownloadById("rec_123") } returns null
        coEvery { recordingRepository.getRecording("rec_123") } returns Result.Success(initialDetail) andThen Result.Success(refreshedDetail)

        val savedStateHandle = SavedStateHandle(mapOf("recordingId" to "rec_123"))
        val viewModel = PlayerViewModel(
            recordingRepository,
            recordingDao,
            downloadDao,
            savedStateHandle
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate token expiry at 12 minutes (720,000 ms)
        viewModel.onPlaybackError(currentPos = 720000L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("https://r2.livevault.app/playback/rec_123.m3u8?token=fresh_new", state.playbackMediaUri)
        assertEquals(720000L, state.currentPositionMs)
    }
}
