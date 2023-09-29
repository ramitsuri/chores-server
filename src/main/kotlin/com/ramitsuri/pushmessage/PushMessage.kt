package com.ramitsuri.pushmessage

data class PushMessage(
    val payload: PushMessagePayload,
    val recipientToken: String,
)
