package com.livevault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val type: String, // RECORDING_STARTED, RECORDING_FINISHED, RECORDING_FAILED, SYSTEM
    val title: String,
    val message: String,
    val recordingId: String?,
    val channelId: String?,
    val isRead: Boolean = false,
    val createdAt: String
)
