package com.livevault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val recordingId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String?,
    val localPath: String,
    val fileSizeBytes: Long = 0L,
    val bytesDownloaded: Long = 0L,
    val progress: Int = 0, // 0 to 100%
    val status: DownloadStatus = DownloadStatus.PENDING,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
