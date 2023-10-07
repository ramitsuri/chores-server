package com.ramitsuri.pushmessage

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.ramitsuri.extensions.Loggable

class FirebasePushMessageDispatcher : PushMessageDispatcher, Loggable {
    override val log = logger()
    private val messaging: FirebaseMessaging

    init {
        val serviceAccount = object {}.javaClass.getResourceAsStream("/google.json")
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        FirebaseApp.initializeApp(options)
        messaging = FirebaseMessaging.getInstance(FirebaseApp.getInstance())
    }

    override fun sendData(deviceToken: String, data: Map<String, String>) {
        try {
            val message = Message.builder()
                .setToken(deviceToken)
                .putAllData(data)
                .build()
            messaging.send(message)
        } catch (e: Exception) {
            log.warning("Failed to dispatch push notification: $e")
        }
    }

    override fun sendNotification(deviceToken: String, notificationContent: NotificationContent) {
        try {
            val notification = Notification.builder()
                .setTitle(notificationContent.title)
                .setBody(notificationContent.body)
                .build()
            val message = Message.builder()
                .setToken(deviceToken)
                .setNotification(notification)
                .build()
            messaging.send(message)
        } catch (e: Exception) {
            log.warning("Failed to dispatch push notification: $e")
        }
    }
}