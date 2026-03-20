// File: EduSoul/app/src/main/java/com/aquaa/edusoul/MyFirebaseMessagingService.kt
package com.aquaa.edusoul

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aquaa.edusoul.activities.messages.MessagesActivity // Ensure this import is correct
import com.aquaa.edusoul.auth.AuthManager
import com.aquaa.edusoul.repositories.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        // Define Notification Channel IDs and Names
        private const val CHANNEL_ID_CHAT = "chat_messages_channel"
        private const val CHANNEL_NAME_CHAT = "Chat Messages"
        private const val CHANNEL_ID_INSTITUTE_ARRIVAL = "institute_arrival_channel"
        private const val CHANNEL_NAME_INSTITUTE_ARRIVAL = "Institute Arrival Notifications"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload. This is where structured messages like arrival notifications will be.
        if (remoteMessage.data.isNotEmpty()) {
            val messageType = remoteMessage.data["type"]
            Log.d(TAG, "Message data payload type: $messageType")

            serviceScope.launch {
                val authManager = AuthManager(applicationContext)
                val currentUser = authManager.getLoggedInUser()

                if (currentUser == null) {
                    Log.d(TAG, "Not showing notification: Current user not logged in.")
                    return@launch
                }

                when (messageType) {
                    "institute_arrival" -> {
                        val parentUserId = remoteMessage.data["parentUserId"]
                        val studentId = remoteMessage.data["studentId"] // Can be used for deep linking
                        val attendanceId = remoteMessage.data["attendanceId"] // Can be used for deep linking

                        // Ensure this notification is for the current logged-in user (parent)
                        if (currentUser.id == parentUserId) {
                            val notificationTitle = remoteMessage.notification?.title ?: "Child Arrived!"
                            val notificationBody = remoteMessage.notification?.body ?: "Your child has arrived at the institute."

                            // For institute arrival, always show notification as it's a critical update
                            sendNotification(
                                notificationTitle,
                                notificationBody,
                                "system_arrival", // A consistent senderId for this type of notification
                                parentUserId,
                                CHANNEL_ID_INSTITUTE_ARRIVAL,
                                CHANNEL_NAME_INSTITUTE_ARRIVAL,
                                studentId?.hashCode() ?: 0 // Use studentId hash for unique notification per student (per day if desired, for multiple arrivals)
                            )
                        } else {
                            Log.d(TAG, "Not showing institute arrival notification: Current user is not the intended recipient.")
                        }
                    }
                    // Default handling for chat messages or any other generic data message types
                    else -> {
                        val senderId = remoteMessage.data["senderId"]
                        val receiverId = remoteMessage.data["receiverId"]
                        val messageContent = remoteMessage.data["messageContent"]
                        val senderName = remoteMessage.data["senderName"] ?: "Unknown Sender"

                        if (currentUser.id == receiverId) {
                            if (!messageContent.isNullOrBlank()) {
                                // Only show notification if the MessagesActivity is NOT currently active
                                // and showing the chat with this specific sender.
                                if (MessagesActivity.isActivityActive && MessagesActivity.activeChatOtherUserId == senderId) {
                                    Log.d(TAG, "Not showing chat notification: User is already in the active chat with this sender.")
                                } else {
                                    sendNotification(
                                        "New message from $senderName",
                                        messageContent,
                                        senderId,
                                        receiverId,
                                        CHANNEL_ID_CHAT,
                                        CHANNEL_NAME_CHAT,
                                        senderId?.hashCode() ?: 0 // Use senderId hash for chat notifications
                                    )
                                }
                            } else {
                                Log.d(TAG, "Not showing chat notification: Message content is blank.")
                            }
                        } else {
                            Log.d(TAG, "Not showing chat notification: Current user not the recipient.")
                        }
                    }
                }
            }
        } else {
            // This block handles messages that ONLY contain a notification payload (without a data payload).
            // Our Firebase Cloud Function sends both `notification` and `data` payloads,
            // so the above `if (remoteMessage.data.isNotEmpty())` block will typically be hit.
            // However, this is a good fallback for other types of FCM notifications.
            remoteMessage.notification?.let {
                Log.d(TAG, "Received notification-only message. Title: ${it.title}, Body: ${it.body}")
                // You might choose to display these with a generic channel or specific logic here.
                // For simplicity, we'll let the structured data messages drive primary display.
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Cancel the coroutine scope when the service is destroyed
    }

    /**
     * Sends a notification to the device.
     * @param title The title of the notification.
     * @param messageBody The body/content of the notification.
     * @param senderIdForIntent Used for intent data (e.g., to open a specific chat). Can be null for system notifications.
     * @param receiverIdForIntent Used for intent data (e.g., the current user's ID). Can be null for system notifications.
     * @param channelId The ID of the notification channel.
     * @param channelName The user-visible name of the notification channel.
     * @param notificationUniqueId A unique integer ID for this specific notification.
     */
    private fun sendNotification(
        title: String,
        messageBody: String,
        senderIdForIntent: String?,
        receiverIdForIntent: String?,
        channelId: String,
        channelName: String,
        notificationUniqueId: Int
    ) {
        // The intent determines where to navigate when the user taps the notification.
        // For institute arrival, it might be better to navigate to a parent dashboard or attendance history.
        // For chat, it should open the MessagesActivity with the correct conversation.
        val intent = Intent(this, MessagesActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Pass necessary data for MessagesActivity to open the correct conversation, or for a different activity
            if (senderIdForIntent != null && receiverIdForIntent != null) {
                putExtra("CURRENT_USER_ID", receiverIdForIntent)
                putExtra("OTHER_USER_ID", senderIdForIntent)
                putExtra("OTHER_USER_NAME", title) // Use the title as sender name for general cases
            }

            // Example for redirecting institute arrival to ParentDashboard (needs more setup in ParentDashboardActivity)
            // if (channelId == CHANNEL_ID_INSTITUTE_ARRIVAL) {
            //     setClass(this@MyFirebaseMessagingService, ParentDashboardActivity::class.java)
            // }
        }

        // Use a unique request code for each PendingIntent to ensure they are distinct
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationUniqueId, // Use the provided unique ID as the request code
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_noti) // Ensure this icon exists and is suitable
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true) // Automatically closes the notification when the user taps it
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for important notifications

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel, but only on API 26+ (Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH // Importance level for the channel
            )
            // You can customize channel description here if needed:
            // channel.description = "Description for this channel"
            notificationManager.createNotificationChannel(channel)
        }

        // Display the notification
        notificationManager.notify(notificationUniqueId, notificationBuilder.build())
    }

    private fun sendRegistrationToServer(token: String?) {
        serviceScope.launch {
            val authManager = AuthManager(applicationContext)
            val userRepository = UserRepository()
            val currentUser = authManager.getLoggedInUser()

            if (currentUser != null && token != null) {
                // Update the user's FCM token in Firestore
                val updatedUser = currentUser.copy(fcmToken = token)
                userRepository.updateUser(updatedUser)
                Log.d(TAG, "FCM token updated in Firestore for user: ${currentUser.id}")
            }
        }
    }
}