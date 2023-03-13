package com.ramitsuri.pushmessage

interface PushMessageService {

    fun sendData(deviceToken: String, data: Map<String, String>)

    fun sendNotification(deviceToken: String, notificationContent: NotificationContent)
}

data class NotificationContent(val title: String, val body: String)