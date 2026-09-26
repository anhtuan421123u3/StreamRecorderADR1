package com.livevault.feature.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.livevault.core.common.Result
import com.livevault.core.database.dao.DownloadDao
import com.livevault.core.database.dao.RecordingDao
import com.livevault.core.database.entity.DownloadEntity
import com.livevault.core.database.entity.DownloadStatus
import com.livevault.core.database.entity.RecordingEntity
import com.livevault.core.network.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class LibraryTab {
    ALL,
    DOWNLOADED
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.ALL,
    val isRefreshing: Boolean = false,
    val recordings: List<RecordingEntity> = emptyList(),
    val downloads: Map<String, DownloadEntity> = emptyMap(),
    val errorMessage: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val recordingDao: RecordingDao,
    private val downloadDao: DownloadDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LibraryTab.ALL)
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        _selectedTab,
        _isRefreshing,
        recordingDao.getAllRecordings(),
        downloadDao.getAllDownloads(),
        _errorMessage
    ) { tab, isRef, recs, dlList, err ->
        val dlMap = dlList.associateBy { it.recordingId }
        val filtered = when (tab) {
            LibraryTab.ALL -> recs
            LibraryTab.DOWNLOADED -> recs.filter { rec ->
                val dl = dlMap[rec.id]
                dl?.status == DownloadStatus.COMPLETED && File(dl.localPath).exists()
            }
        }
        LibraryUiState(
            selectedTab = tab,
            isRefreshing = isRef,
            recordings = filtered,
            downloads = dlMap,
            errorMessage = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    init {
        syncRecordings()
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun refresh() {
        syncRecordings(isPullToRefresh = true)
    }

    fun syncRecordings(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullToRefresh) _isRefreshing.value = true
            when (val res = recordingRepository.getRecordings(page = 1, limit = 50)) {
                is Result.Success -> {
                    val entities = res.data.recordings.map { dto ->
                        RecordingEntity(
                            id = dto.id,
                            channelId = dto.channelId,
                            channelName = dto.channelName,
                            channelAvatar = dto.channelAvatar,
                            title = dto.title,
                            durationSeconds = dto.durationSeconds,
                            sizeBytes = dto.sizeBytes,
                            status = dto.status,
                            recordedAt = dto.recordedAt,
                            thumbnailUrl = dto.thumbnailUrl,
                            hlsManifestKey = dto.hlsManifestKey
                        )
                    }
                    recordingDao.insertRecordings(entities)
                }
                is Result.Error -> {
                    _errorMessage.value = res.throwable.message
                }
                is Result.Loading -> {}
            }
            if (isPullToRefresh) _isRefreshing.value = false
        }
    }

    /**
     * Trigger background download via WorkManager
     */
    fun startDownload(recording: RecordingEntity) {
        viewModelScope.launch {
            // Save initial download entity in Room
            downloadDao.insertOrUpdate(
                DownloadEntity(
                    recordingId = recording.id,
                    title = recording.title,
                    channelName = recording.channelName,
                    thumbnailUrl = recording.thumbnailUrl,
                    localPath = "",
                    fileSizeBytes = recording.sizeBytes,
                    bytesDownloaded = 0L,
                    progress = 0,
                    status = DownloadStatus.PENDING
                )
            )

            // Enqueue DownloadRecordingWorker
            val inputData = Data.Builder()
                .putString("KEY_RECORDING_ID", recording.id)
                .putString("KEY_TITLE", recording.title)
                .putString("KEY_CHANNEL_NAME", recording.channelName)
                .putLong("KEY_TOTAL_BYTES", recording.sizeBytes)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<androidx.work.ListenableWorker>()
                .setInputData(inputData)
                .addTag("download_${recording.id}")
                .build()

            // In actual app, DownloadRecordingWorker is bound in :app module
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    fun deleteRecording(recordingId: String) {
        viewModelScope.launch {
            // 1. Delete from Room and local file
            val dl = downloadDao.getDownloadById(recordingId)
            if (dl != null && dl.localPath.isNotBlank()) {
                val f = File(dl.localPath)
                if (f.exists()) f.delete()
                downloadDao.deleteById(recordingId)
            }
            recordingDao.deleteRecordingById(recordingId)

            // 2. Delete from remote backend
            recordingRepository.deleteRecording(recordingId)
        }
    }
}
