package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.RepeatUnit
import java.time.LocalDateTime

interface TasksTaskAssignmentsRepository {
    suspend fun edit(
        taskId: String,
        name: String,
        description: String,
        dueDate: LocalDateTime,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        repeatEndDate: LocalDateTime?,
        rotateMember: Boolean,
        status: ActiveStatus
    ): Boolean

    suspend fun delete(taskId: String): Boolean
}