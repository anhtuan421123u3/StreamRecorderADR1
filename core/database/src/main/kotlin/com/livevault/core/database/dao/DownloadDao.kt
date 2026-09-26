package com.livevault.core.database.dao

import androidx.room.*
import com.livevault.core.database.entity.DownloadEntity
import com.livevault.core.database.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'DOWNLOADING')")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE recordingId = :recordingId")
    suspend fun getDownloadById(recordingId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE recordingId = :recordingId")
    fun getDownloadFlow(recordingId: String): Flow<DownloadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(download: DownloadEntity)

    @Query("""
        UPDATE downloads 
        SET bytesDownloaded = :bytesDownloaded, 
            fileSizeBytes = :fileSizeBytes, 
            progress = :progress, 
            status = :status 
        WHERE recordingId = :recordingId
    """)
    suspend fun updateProgress(
        recordingId: String,
        bytesDownloaded: Long,
        fileSizeBytes: Long,
        progress: Int,
        status: DownloadStatus
    )

    @Query("""
        UPDATE downloads 
        SET status = :status, 
            completedAt = :completedAt,
            localPath = :localPath,
            progress = 100
        WHERE recordingId = :recordingId
    """)
    suspend fun markCompleted(
        recordingId: String,
        localPath: String,
        status: DownloadStatus = DownloadStatus.COMPLETED,
        completedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE downloads 
        SET status = :status, 
            errorMessage = :error 
        WHERE recordingId = :recordingId
    """)
    suspend fun markFailed(
        recordingId: String,
        error: String,
        status: DownloadStatus = DownloadStatus.FAILED
    )

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE recordingId = :recordingId")
    suspend fun deleteById(recordingId: String)
}
