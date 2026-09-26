package com.livevault.core.database.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.livevault.core.database.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Query("SELECT * FROM recordings ORDER BY recordedAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings ORDER BY recordedAt DESC")
    fun getRecordingsPagingSource(): PagingSource<Int, RecordingEntity>

    @Query("SELECT * FROM recordings WHERE channelId = :channelId ORDER BY recordedAt DESC")
    fun getRecordingsByChannel(channelId: String): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE isDownloaded = 1 ORDER BY recordedAt DESC")
    fun getDownloadedRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getRecordingFlow(id: String): Flow<RecordingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordings(recordings: List<RecordingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query("UPDATE recordings SET isDownloaded = :isDownloaded, localPath = :localPath WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localPath: String?)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: String)

    @Query("DELETE FROM recordings")
    suspend fun clearAll()
}
