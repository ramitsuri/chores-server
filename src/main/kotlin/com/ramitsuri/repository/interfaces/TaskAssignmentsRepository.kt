package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.models.TaskAssignmentDto
import java.time.Instant
import java.time.LocalDateTime

interface TaskAssignmentsRepository {
    suspend fun add(taskAssignments: List<TaskAssignmentInsert>)

    suspend fun edit(taskAssignments: List<TaskAssignmentDto>, requesterMemberId: String): List<String>

    // Edit will be applied if the assignment is assigned to the requester member
    suspend fun editOwn(taskAssignments: List<TaskAssignmentDto>, requesterMemberId: String): List<String>

    // Edit will be applied if the assignment's task belongs to requester member's houses
    suspend fun editForHouse(
        taskAssignments: List<TaskAssignmentDto>,
        houseIds: List<String>,
        requesterMemberId: String
    ): List<String>

    suspend fun getMostRecentForTask(taskId: String): TaskAssignment?

    suspend fun get(taskAssignmentIds: List<String>): List<TaskAssignment>

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

data class TaskAssignmentInsert(
    val progressStatus: ProgressStatus,
    val progressStatusDateTime: Instant,
    val taskId: String,
    val memberId: String,
    val dueDateTime: LocalDateTime,
    val createdDateTime: Instant,
    val createType: CreateType
) {
    constructor(taskAssignment: TaskAssignment) : this(
        progressStatus = taskAssignment.progressStatus,
        progressStatusDateTime = taskAssignment.progressStatusDate,
        taskId = taskAssignment.task.id,
        memberId = taskAssignment.member.id,
        dueDateTime = taskAssignment.dueDateTime,
        createdDateTime = taskAssignment.createdDate,
        createType = taskAssignment.createType
    )
}