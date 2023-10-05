package com.ramitsuri.events

sealed class Event {
    data class AssignmentsAdded(val assignmentIds: List<String>) : Event()

    data class TaskEdited(val taskId: String) : Event()

    data class AssignmentsUpdated(val assignmentIds: List<String>) : Event()

    data object TaskNeedsAssignments: Event()
}