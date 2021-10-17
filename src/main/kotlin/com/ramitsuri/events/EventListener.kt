package com.ramitsuri.events

interface EventListener {

    fun eventReceived(event: Event)
}