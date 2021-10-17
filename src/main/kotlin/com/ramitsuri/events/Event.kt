package com.ramitsuri.events

import com.ramitsuri.models.TaskAssignment

sealed class Event {
    data class AssignmentsAdded(val assignments: List<TaskAssignment>): Event()
    data class AssignmentsUpdated(val assignments: List<TaskAssignment>): Event()
}