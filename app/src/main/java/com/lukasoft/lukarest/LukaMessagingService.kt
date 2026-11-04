package com.lukasoft.lukarest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import com.lukasoft.lukarest.data.store.AuthStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class LukaMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        val id = message.data["id"] ?: return
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        val channelId = "operations"
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "Operasyon bildirimleri", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val requestCode = (type to id).hashCode()
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("type", type)
            .putExtra("id", id)
        val open = PendingIntent.getActivity(this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(message.notification?.title ?: "LukaRest")
            .setContentText(message.notification?.body ?: "Yeni operasyon bildirimi")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(requestCode, notification)
        } catch (_: SecurityException) {
            // Notification permission may have changed after the initial check.
        }
    }

    override fun onRegistered(installationId: String) {
        if (AuthStore.accessToken.value.isNullOrBlank()) return
        runBlocking(Dispatchers.IO) {
            AuthStore.registerCurrentDevice(
                installationId,
                NotificationManagerCompat.from(this@LukaMessagingService).areNotificationsEnabled()
            )
        }
    }
}

