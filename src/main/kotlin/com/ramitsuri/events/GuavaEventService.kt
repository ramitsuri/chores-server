package com.ramitsuri.events

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import java.util.concurrent.ConcurrentHashMap

class GuavaEventService: EventService {

    private val bus = EventBus()
    private val listeners = ConcurrentHashMap.newKeySet<EventListener>()

    init {
        bus.register(object: EventListener {
            @Subscribe
            override fun eventReceived(event: Event) {
                for (listener in listeners) {
                    listener.eventReceived(event)
                }
            }
        })
    }

    override fun post(event: Event) {
        bus.post(event)
    }

    override fun register(eventListener: EventListener) {
        listeners.add(eventListener)
    }

    override fun unregister(eventListener: EventListener) {
        listeners.remove(eventListener)
    }
}