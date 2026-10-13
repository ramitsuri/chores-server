package com.ramitsuri.pushmessage

import com.ramitsuri.extensions.Loggable

class NoOpPushMessageDispatcher : PushMessageDispatcher, Loggable {
    override val log = logger()

    override fun sendData(deviceToken: String, data: Map<String, String>) {
        log.info("Push messages disabled, not sending data: $data")
    }

    override fun sendNotification(deviceToken: String, notificationContent: NotificationContent) {
        log.info("Push messages disabled, not sending notification: $notificationContent")
    }
}
