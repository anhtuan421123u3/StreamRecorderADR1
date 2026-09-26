package com.livevault.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.livevault.core.database.dao.ChannelDao
import com.livevault.core.database.dao.DownloadDao
import com.livevault.core.database.dao.NotificationDao
import com.livevault.core.database.dao.RecordingDao
import com.livevault.core.database.entity.ChannelEntity
import com.livevault.core.database.entity.DownloadEntity
import com.livevault.core.database.entity.NotificationEntity
import com.livevault.core.database.entity.RecordingEntity

@Database(
    entities = [
        ChannelEntity::class,
        RecordingEntity::class,
        DownloadEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LiveVaultDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun recordingDao(): RecordingDao
    abstract fun downloadDao(): DownloadDao
    abstract fun notificationDao(): NotificationDao
}
