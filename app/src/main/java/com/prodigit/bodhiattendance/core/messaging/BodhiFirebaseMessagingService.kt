package com.prodigit.bodhiattendance.core.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.prodigit.bodhiattendance.MainActivity
import com.prodigit.bodhiattendance.R
import com.prodigit.bodhiattendance.core.device.DeviceIdManager
import com.prodigit.bodhiattendance.core.network.RetrofitClient
import com.prodigit.bodhiattendance.core.session.AndroidTokenManager
import com.prodigit.bodhiattendance.core.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles incoming Firebase Cloud Messaging push notifications and FCM token refreshes.
 */
class BodhiFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM token received")
        sendTokenToBackend(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "FCM message received: ${remoteMessage.messageId}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Bodhi Attendance Update"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["description"]
            ?: "Attendance status updated"

        showNotification(title, body, remoteMessage.data)
    }

    private fun showNotification(title: String, message: String, data: Map<String, String> = emptyMap()) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "bodhi_push_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Attendance Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Push notifications for attendance check-ins, breaks, and regularizations"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    companion object {
        private const val TAG = "BODHI_FCM"

        fun sendTokenToBackend(context: Context, fcmToken: String) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    val userId = SessionManager(context).getUserId()
                    if (userId <= 0) return@launch

                    val deviceId = DeviceIdManager(context).getDeviceId() ?: return@launch
                    val tokenManager = AndroidTokenManager(context)
                    val accessToken = tokenManager.usableAccessToken() ?: return@launch

                    // Call backend FCM token endpoint
                    val response = RetrofitClient.attendanceNotificationApi.registerFcmToken(
                        token = "Bearer $accessToken",
                        body = FcmTokenRegistrationRequest(device_id = deviceId, fcm_token = fcmToken)
                    )

                    if (response.isSuccessful) {
                        Log.i(TAG, "FCM token registered with backend successfully")
                    } else {
                        Log.w(TAG, "FCM token registration rejected: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send FCM token to backend: ${e.javaClass.simpleName}")
                }
            }
        }

        fun fetchAndRegisterFcmToken(context: Context) {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful && task.result != null) {
                            sendTokenToBackend(context.applicationContext, task.result)
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch FCM token: ${e.javaClass.simpleName}")
            }
        }
    }
}

data class FcmTokenRegistrationRequest(
    val device_id: String,
    val fcm_token: String
)
