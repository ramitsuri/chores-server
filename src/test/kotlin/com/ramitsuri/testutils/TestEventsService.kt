package com.ramitsuri.testutils

import com.ramitsuri.events.Event
import com.ramitsuri.events.EventListener
import com.ramitsuri.events.EventService

class TestEventsService: EventService {
    private val events = mutableListOf<Event>()

    override fun post(event: Event) {
        events.add(event)
    }

    override fun register(eventListener: EventListener) {
    }

    override fun unregister(eventListener: EventListener) {
    }

    fun getEvents() = events
}