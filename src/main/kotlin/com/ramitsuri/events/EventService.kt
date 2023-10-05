package com.ramitsuri.events

import kotlinx.coroutines.flow.SharedFlow

interface EventService {

    val events: SharedFlow<Event>

    suspend fun post(vararg events: Event)
}