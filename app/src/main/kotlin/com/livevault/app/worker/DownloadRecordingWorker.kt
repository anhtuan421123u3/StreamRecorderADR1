package com.livevault.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.livevault.core.common.Result as AppResult
import com.livevault.core.database.dao.DownloadDao
import com.livevault.core.database.dao.RecordingDao
import com.livevault.core.database.entity.DownloadStatus
import com.livevault.core.network.repository.RecordingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile

@HiltWorker
class DownloadRecordingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val recordingRepository: RecordingRepository,
    private val downloadDao: DownloadDao,
    private val recordingDao: RecordingDao,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "downloads_channel"
        const val CHANNEL_NAME = "Stream Downloads"
        const val KEY_RECORDING_ID = "KEY_RECORDING_ID"
        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_CHANNEL_NAME = "KEY_CHANNEL_NAME"
        const val KEY_PROGRESS = "KEY_PROGRESS"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): androidx.work.ListenableWorker.Result = withContext(Dispatchers.IO) {
        val recordingId = inputData.getString(KEY_RECORDING_ID)
            ?: return@withContext androidx.work.ListenableWorker.Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Recording"

        createNotificationChannel()
        val notificationId = recordingId.hashCode()

        // Set worker as foreground
        try {
            setForeground(createForegroundInfo(notificationId, title, 0))
        } catch (e: Exception) {
            // Foreground permission or background restriction fallback
        }

        // 1. Fetch 1h signed download URL from backend
        val downloadUrlRes = recordingRepository.getDownloadUrl(recordingId)
        val downloadUrl = if (downloadUrlRes is AppResult.Success) {
            downloadUrlRes.data.downloadUrl
        } else {
            downloadDao.markFailed(recordingId, "Failed to fetch download URL")
            return@withContext androidx.work.ListenableWorker.Result.retry()
        }

        // 2. Prepare target file in app external files (scoped storage)
        val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir
        val targetFile = File(moviesDir, "livevault_${recordingId}.mp4")

        val existingBytes = if (targetFile.exists()) targetFile.length() else 0L

        // 3. Build OkHttp request with Range header for resuming
        val requestBuilder = Request.Builder().url(downloadUrl)
        if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        try {
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                downloadDao.markFailed(recordingId, "Server returned HTTP ${response.code}")
                return@withContext androidx.work.ListenableWorker.Result.retry()
            }

            val responseBody = response.body
                ?: return@withContext androidx.work.ListenableWorker.Result.failure()
            val contentLength = responseBody.contentLength()
            val totalBytes = if (existingBytes > 0 && response.code == 206) {
                existingBytes + contentLength
            } else {
                contentLength
            }

            downloadDao.updateProgress(
                recordingId = recordingId,
                bytesDownloaded = existingBytes,
                fileSizeBytes = totalBytes,
                progress = if (totalBytes > 0) ((existingBytes * 100) / totalBytes).toInt() else 0,
                status = DownloadStatus.DOWNLOADING
            )

            // 4. Stream response and write to disk
            val inputStream: InputStream = responseBody.byteStream()
            val outputStream = RandomAccessFile(targetFile, "rw")
            if (existingBytes > 0 && response.code == 206) {
                outputStream.seek(existingBytes)
            }

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int = inputStream.read(buffer)
            var downloaded = existingBytes
            var lastUpdateProgress = -1

            while (bytesRead != -1) {
                if (isStopped) {
                    outputStream.close()
                    inputStream.close()
                    downloadDao.updateProgress(
                        recordingId = recordingId,
                        bytesDownloaded = downloaded,
                        fileSizeBytes = totalBytes,
                        progress = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0,
                        status = DownloadStatus.PAUSED
                    )
                    return@withContext androidx.work.ListenableWorker.Result.retry()
                }

                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead

                val currentProgress = if (totalBytes > 0) ((downloaded * 100) / totalBytes).toInt() else 0
                if (currentProgress != lastUpdateProgress) {
                    lastUpdateProgress = currentProgress
                    setProgress(workDataOf(KEY_PROGRESS to currentProgress))

                    // Update Room progress
                    downloadDao.updateProgress(
                        recordingId = recordingId,
                        bytesDownloaded = downloaded,
                        fileSizeBytes = totalBytes,
                        progress = currentProgress,
                        status = DownloadStatus.DOWNLOADING
                    )

                    // Update Notification
                    notificationManager.notify(
                        notificationId,
                        buildNotification(title, currentProgress).build()
                    )
                }

                bytesRead = inputStream.read(buffer)
            }

            outputStream.close()
            inputStream.close()

            // 5. Mark completed in Room
            downloadDao.markCompleted(
                recordingId = recordingId,
                localPath = targetFile.absolutePath,
                status = DownloadStatus.COMPLETED
            )
            recordingDao.updateDownloadStatus(
                id = recordingId,
                isDownloaded = true,
                localPath = targetFile.absolutePath
            )

            // Complete notification
            val completeNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Download Complete")
                .setContentText(title)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(notificationId, completeNotification)

            androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            downloadDao.markFailed(recordingId, e.message ?: "Download error")
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    private fun createForegroundInfo(notificationId: Int, title: String, progress: Int): ForegroundInfo {
        val notification = buildNotification(title, progress).build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun buildNotification(title: String, progress: Int): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading stream")
            .setContentText("$title ($progress%)")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setSilent(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress for recorded streams"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
