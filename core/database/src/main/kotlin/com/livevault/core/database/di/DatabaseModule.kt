package com.livevault.core.database.di

import android.content.Context
import androidx.room.Room
import com.livevault.core.database.LiveVaultDatabase
import com.livevault.core.database.dao.ChannelDao
import com.livevault.core.database.dao.DownloadDao
import com.livevault.core.database.dao.NotificationDao
import com.livevault.core.database.dao.RecordingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LiveVaultDatabase {
        return Room.databaseBuilder(
            context,
            LiveVaultDatabase::class.java,
            "livevault.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideChannelDao(database: LiveVaultDatabase): ChannelDao = database.channelDao()

    @Provides
    fun provideRecordingDao(database: LiveVaultDatabase): RecordingDao = database.recordingDao()

    @Provides
    fun provideDownloadDao(database: LiveVaultDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideNotificationDao(database: LiveVaultDatabase): NotificationDao = database.notificationDao()
}
