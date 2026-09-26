package com.livevault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey
    val id: String,
    val channelId: String,
    val channelName: String,
    val channelAvatar: String?,
    val title: String,
    val durationSeconds: Long = 0L,
    val sizeBytes: Long = 0L,
    val status: String, // RECORDING, PROCESSING, READY, FAILED
    val recordedAt: String,
    val thumbnailUrl: String?,
    val hlsManifestKey: String?,
    val isDownloaded: Boolean = false,
    val localPath: String? = null
)
