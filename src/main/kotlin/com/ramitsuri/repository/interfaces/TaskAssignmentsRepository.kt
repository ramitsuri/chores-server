package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.models.TaskAssignmentDto
import java.time.Instant
import java.time.LocalDateTime

interface TaskAssignmentsRepository {
    suspend fun add(
        progressStatus: ProgressStatus,
        statusDate: Instant,
        taskId: String,
        memberId: String,
        dueDate: LocalDateTime,
        createdDate: Instant,
        createType: CreateType
    ): TaskAssignment?

    suspend fun edit(taskAssignments: List<TaskAssignmentDto>, requesterMemberId: String): List<String>

    // Edit will be applied if the assignment is assigned to the requester member
    suspend fun editOwn(taskAssignments: List<TaskAssignmentDto>, requesterMemberId: String): List<String>

    // Edit will be applied if the assignment's task belongs to requester member's houses
    suspend fun editForHouse(
        taskAssignments: List<TaskAssignmentDto>,
        houseIds: List<String>,
        requesterMemberId: String
    ): List<String>

    suspend fun get(): List<TaskAssignment>

    suspend fun get(filter: TaskAssignmentFilter): List<TaskAssignment>

    // Get if the assignment's task belongs to requester member's houses
    suspend fun getForHouse(filter: TaskAssignmentFilter, houseIds: List<String>): List<TaskAssignment>
}

data class TaskAssignmentFilter(
    val memberId: String? = null,
    val notMemberId: String? = null,
    val progressStatus: ProgressStatus = ProgressStatus.UNKNOWN,
    val onlyActiveAndPausedHouse: Boolean
)