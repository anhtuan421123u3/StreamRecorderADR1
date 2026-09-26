package com.livevault.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.livevault.app.MainActivity
import com.livevault.core.database.dao.NotificationDao
import com.livevault.core.database.entity.NotificationEntity
import com.livevault.core.network.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class LiveVaultFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var notificationDao: NotificationDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val LIVEVAULT_CHANNEL_ID = "livevault_events"
        const val LIVEVAULT_CHANNEL_NAME = "LiveVault Livestream Alerts"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            notificationRepository.registerDeviceToken(token, platform = "ANDROID")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "LiveVault Alert"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: ""
        val type = remoteMessage.data["type"] ?: "SYSTEM"
        val recordingId = remoteMessage.data["recordingId"]
        val channelId = remoteMessage.data["channelId"]

        // 1. Cache notification in Room database
        val notifId = UUID.randomUUID().toString()
        serviceScope.launch {
            notificationDao.insertNotification(
                NotificationEntity(
                    id = notifId,
                    type = type,
                    title = title,
                    message = body,
                    recordingId = recordingId,
                    channelId = channelId,
                    isRead = false,
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }

        // 2. Show System Push Notification
        showNotification(title, body, recordingId)
    }

    private fun showNotification(title: String, message: String, recordingId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (recordingId != null) {
                putExtra("KEY_RECORDING_ID", recordingId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, LIVEVAULT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LIVEVAULT_CHANNEL_ID,
                LIVEVAULT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live streaming started/finished notifications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
