package com.ramitsuri.events

import com.ramitsuri.models.TaskAssignment

sealed class Event {
    data class AssignmentsAdded(val assignmentIds: List<String>) : Event()

    data class TaskEdited(val taskId: String) : Event()

    data class AssignmentsUpdated(val assignmentIds: List<String>) : Event()
}