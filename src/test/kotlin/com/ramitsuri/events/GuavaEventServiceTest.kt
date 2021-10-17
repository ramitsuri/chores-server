package com.ramitsuri.events

import org.junit.Test
import kotlin.test.assertEquals

class GuavaEventServiceTest {
    private val service = GuavaEventService()

    @Test
    fun testEventReceiver() {
        var eventCount1 = 0
        val listener1 = object: EventListener {
            override fun eventReceived(event: Event) {
                eventCount1++
            }
        }

        var eventCount2 = 0
        val listener2 = object: EventListener {
            override fun eventReceived(event: Event) {
                eventCount2++
            }
        }
        service.register(listener1)
        service.register(listener2)
        service.post(Event.AssignmentsUpdated(listOf()))
        assertEquals(1, eventCount1)
        assertEquals(1, eventCount2)

        eventCount1 = 0
        eventCount2 = 0
        service.unregister(listener1)
        service.post(Event.AssignmentsUpdated(listOf()))
        assertEquals(0, eventCount1)
        assertEquals(1, eventCount2)
    }
}