package com.ramitsuri.pushmessage

import com.ramitsuri.events.Event
import com.ramitsuri.events.EventService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class PushMessageService(
    private val pushMessageDispatcher: PushMessageDispatcher,
    private val pushMessagePayloadGenerator: PushMessagePayloadGenerator,
    coroutineScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    eventService: EventService,
) {

    private val relevantEvents =
        listOf(Event.AssignmentsAdded::class, Event.AssignmentsAdded::class, Event.TaskEdited::class)

    init {
        coroutineScope.launch(ioDispatcher) {
            eventService.events.filter { it::class in relevantEvents }.collect { event ->
                eventReceived(event)
            }
        }
    }

    private suspend fun eventReceived(event: Event) {
        when (event) {
            is Event.AssignmentsAdded -> {
                forTaskAssignments(event.assignmentIds)
            }

            is Event.AssignmentsUpdated -> {
                forTaskAssignments(event.assignmentIds)
            }

            is Event.TaskEdited -> {
                forTask(event.taskId)
            }

            else -> {
                // Nothing
            }
        }
    }

    private suspend fun forTask(taskId: String) {
        val pushMessages = pushMessagePayloadGenerator.getForTasks(listOf(taskId))
        pushMessages.forEach { pushMessage ->
            val data = pushMessage.payload.toMap()
            pushMessageDispatcher.sendData(pushMessage.recipientToken, data)
        }
    }

    private suspend fun forTaskAssignments(taskAssignmentIds: List<String>) {
        val pushMessages = pushMessagePayloadGenerator.getForTaskAssignments(taskAssignmentIds)
        pushMessages.forEach { pushMessage ->
            val data = pushMessage.payload.toMap()
            pushMessageDispatcher.sendData(pushMessage.recipientToken, data)
        }
    }

}