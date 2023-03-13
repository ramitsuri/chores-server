package com.ramitsuri.pushmessage

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import java.io.FileInputStream

class FirebasePushMessageService : PushMessageService {
    private val messaging: FirebaseMessaging

    init {
        val serviceAccount = FileInputStream("./google.json")
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        FirebaseApp.initializeApp(options)
        messaging = FirebaseMessaging.getInstance(FirebaseApp.getInstance())
    }

    override fun sendData(deviceToken: String, data: Map<String, String>) {
        val message = Message.builder()
            .setToken(deviceToken)
            .putAllData(data)
            .build()
        messaging.send(message)
    }

    override fun sendNotification(deviceToken: String, notificationContent: NotificationContent) {
        val notification = Notification.builder()
            .setTitle(notificationContent.title)
            .setBody(notificationContent.body)
            .build()
        val message = Message.builder()
            .setToken(deviceToken)
            .setNotification(notification)
            .build()
        messaging.send(message)
    }
}