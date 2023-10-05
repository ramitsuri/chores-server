package com.ramitsuri.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SystemEventService : EventService {
    private val _events = MutableSharedFlow<Event>()
    override val events = _events.asSharedFlow()

    override suspend fun post(vararg events: Event) {
        events.forEach { event ->
            _events.emit(event)
        }
    }
}