package com.ramitsuri.events

interface EventService {

    fun post(event: Event)
    fun register(eventListener: EventListener)
    fun unregister(eventListener: EventListener)
}