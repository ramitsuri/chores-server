package com.ramitsuri.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SystemEventService : EventService {
    private val _events = MutableSharedFlow<Event>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events = _events.asSharedFlow()

    override suspend fun post(vararg events: Event) {
        events.forEach { event ->
            _events.emit(event)
        }
    }
}