package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.models.TaskAssignmentDto
import java.time.Instant

interface TaskAssignmentsRepository {
    suspend fun add(
        progressStatus: ProgressStatus,
        statusDate: Instant,
        taskId: String,
        memberId: String,
        dueDate: Instant,
        createdDate: Instant,
        createType: CreateType
    ): TaskAssignment?

    suspend fun delete(): Int

    suspend fun delete(id: String): Int

    suspend fun edit(id: String, progressStatus: ProgressStatus, statusDate: Instant): TaskAssignment?

    suspend fun edit(taskAssignments: List<TaskAssignmentDto>): List<String>

    suspend fun get(): List<TaskAssignment>

    suspend fun get(filter: TaskAssignmentFilter): List<TaskAssignment>

    suspend fun get(id: String): TaskAssignment?
}

data class TaskAssignmentFilter(
    val memberId: String? = null,
    val notMemberId: String? = null,
    val progressStatus: ProgressStatus = ProgressStatus.UNKNOWN
)