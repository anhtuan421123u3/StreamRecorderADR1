package com.livevault.feature.home

import com.livevault.core.common.Result
import com.livevault.core.database.dao.ChannelDao
import com.livevault.core.database.dao.RecordingDao
import com.livevault.core.network.dto.ChannelDto
import com.livevault.core.network.dto.ChannelListDto
import com.livevault.core.network.dto.RecordingItemDto
import com.livevault.core.network.dto.RecordingListDto
import com.livevault.core.network.repository.ChannelRepository
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
class HomeViewModelTest {

    private val channelRepository: ChannelRepository = mockk(relaxed = true)
    private val recordingRepository: RecordingRepository = mockk(relaxed = true)
    private val channelDao: ChannelDao = mockk(relaxed = true)
    private val recordingDao: RecordingDao = mockk(relaxed = true)
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
    fun `loadData populates channels and recordings and identifies live status`() = runTest {
        val dummyChannel = ChannelDto(
            id = "ch_1",
            platform = "YOUTUBE",
            channelId = "UC123",
            channelUrl = "https://youtube.com/@test",
            displayName = "Streamer One",
            avatarUrl = null,
            isActive = true,
            isLive = true
        )
        val dummyRecording = RecordingItemDto(
            id = "rec_1",
            channelId = "ch_1",
            channelName = "Streamer One",
            channelAvatar = null,
            title = "Live Stream #1",
            durationSeconds = 3600L,
            sizeBytes = 1024L * 1024 * 500,
            status = "RECORDING",
            recordedAt = "2026-09-22T08:00:00Z",
            thumbnailUrl = null,
            hlsManifestKey = null
        )

        coEvery { channelRepository.getChannels(any(), any()) } returns Result.Success(
            ChannelListDto(channels = listOf(dummyChannel), total = 1, page = 1, limit = 50)
        )
        coEvery { recordingRepository.getRecordings(any(), any(), any(), any(), any(), any()) } returns Result.Success(
            RecordingListDto(recordings = listOf(dummyRecording), total = 1, page = 1, limit = 20)
        )

        val viewModel = HomeViewModel(
            channelRepository,
            recordingRepository,
            channelDao,
            recordingDao
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.channels.size)
        assertEquals(1, state.liveChannels.size)
        assertEquals("Streamer One", state.liveChannels.first().displayName)
        assertEquals(1, state.activeRecordingsCount)
    }
}
