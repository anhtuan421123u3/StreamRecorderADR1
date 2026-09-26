package com.livevault.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey
    val id: String,
    val platform: String,
    val channelId: String,
    val channelUrl: String,
    val displayName: String,
    val avatarUrl: String?,
    val isActive: Boolean = true,
    val isLive: Boolean = false,
    val lastCheckedAt: String? = null,
    val streamTitle: String? = null
)
