package com.livevault.feature.library

import android.content.Context
import app.cash.turbine.test
import com.livevault.core.common.Result
import com.livevault.core.database.dao.DownloadDao
import com.livevault.core.database.dao.RecordingDao
import com.livevault.core.database.entity.DownloadEntity
import com.livevault.core.database.entity.DownloadStatus
import com.livevault.core.database.entity.RecordingEntity
import com.livevault.core.network.dto.RecordingListDto
import com.livevault.core.network.repository.RecordingRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val recordingRepository: RecordingRepository = mockk(relaxed = true)
    private val recordingDao: RecordingDao = mockk(relaxed = true)
    private val downloadDao: DownloadDao = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
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
    fun `observes Room recordings and downloads reactively`() = runTest {
        val dummyRecording = RecordingEntity(
            id = "rec_1",
            channelId = "ch_1",
            channelName = "Channel One",
            channelAvatar = null,
            title = "Test Stream",
            durationSeconds = 600L,
            sizeBytes = 1000000L,
            status = "READY",
            recordedAt = "2026-09-22T08:00:00Z",
            thumbnailUrl = null,
            hlsManifestKey = null
        )

        val dummyDownload = DownloadEntity(
            recordingId = "rec_1",
            title = "Test Stream",
            channelName = "Channel One",
            thumbnailUrl = null,
            localPath = "/fake/path.mp4",
            status = DownloadStatus.COMPLETED
        )

        every { recordingDao.getAllRecordings() } returns flowOf(listOf(dummyRecording))
        every { downloadDao.getAllDownloads() } returns flowOf(listOf(dummyDownload))
        coEvery { recordingRepository.getRecordings(any(), any()) } returns Result.Success(
            RecordingListDto(recordings = emptyList(), total = 0, page = 1, limit = 50)
        )

        val viewModel = LibraryViewModel(
            recordingRepository,
            recordingDao,
            downloadDao,
            context
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.recordings.size)
            assertEquals("rec_1", state.recordings.first().id)
            assertEquals(DownloadStatus.COMPLETED, state.downloads["rec_1"]?.status)
        }
    }
}
