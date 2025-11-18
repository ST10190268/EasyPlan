package com.easyplan.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles Firebase Cloud Messaging push notifications for real-time alerts.
 */
class EasyPlanFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "EasyPlanFcmService"
        const val DEFAULT_TOPIC = "easyplan_tasks"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "onNewToken: Refreshed FCM token $token")
        // Tokens can be forwarded to Firestore/Functions if needed.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "onMessageReceived: ${message.data}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(com.easyplan.R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: ""

        NotificationUtils.showTaskNotification(this, title, body)
    }
}
