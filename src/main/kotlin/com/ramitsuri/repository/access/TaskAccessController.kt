package com.ramitsuri.repository.access

import com.ramitsuri.models.Access
import com.ramitsuri.models.AccessResult
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import com.ramitsuri.repository.interfaces.TasksTaskAssignmentsRepository
import java.time.Instant
import java.time.LocalDateTime

class TaskAccessController(
    private val memberAssignmentsRepository: MemberAssignmentsRepository,
    private val tasksRepository: TasksRepository,
    private val tasksTaskAssignmentsRepository: TasksTaskAssignmentsRepository,
    membersRepository: MembersRepository
) : AccessController(membersRepository) {

    suspend fun get(requesterMemberId: String): AccessResult<List<Task>> {
        return when (getAccess(requesterMemberId)) {
            Access.NONE -> {
                AccessResult.Failure
            }
            Access.READ_HOUSE_WRITE_OWN, Access.READ_HOUSE_WRITE_HOUSE -> {
                val houseIds = getHouseIds(requesterMemberId)
                AccessResult.Success(tasksTaskAssignmentsRepository.getForHouses(houseIds))
            }
            Access.READ_ALL_WRITE_ALL -> {
                AccessResult.Success(tasksTaskAssignmentsRepository.get())
            }
        }
    }

    /**
     * Returns [AccessResult.Success] with null data if the task doesn't exist
     */
    suspend fun get(requesterMemberId: String, taskId: String): AccessResult<Task?> {
        return when (getAccess(requesterMemberId)) {
            Access.NONE -> {
                AccessResult.Failure
            }
            Access.READ_HOUSE_WRITE_OWN, Access.READ_HOUSE_WRITE_HOUSE -> {
                val task = tasksRepository.get(taskId) ?: return AccessResult.Success(null)
                if (task.houseId in getHouseIds(requesterMemberId)) {
                    AccessResult.Success(task)
                } else {
                    AccessResult.Failure
                }
            }
            Access.READ_ALL_WRITE_ALL -> {
                AccessResult.Success(tasksRepository.get(taskId))
            }
        }
    }

    /**
     * Returns [AccessResult.Success] with null data if the task could not be created
     */
    suspend fun add(
        requesterMemberId: String,
        name: String,
        description: String,
        dueDateTime: LocalDateTime,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        repeatEndDateTime: LocalDateTime?,
        houseId: String,
        memberId: String,
        rotateMember: Boolean,
        createdDate: Instant,
        status: ActiveStatus
    ): AccessResult<Task?> {
        val hasAccess = when (getAccess(requesterMemberId)) {
            Access.NONE -> false
            Access.READ_HOUSE_WRITE_OWN -> {
                memberId == requesterMemberId && houseId in getHouseIds(requesterMemberId)
            }
            Access.READ_HOUSE_WRITE_HOUSE -> houseId in getHouseIds(requesterMemberId)
            Access.READ_ALL_WRITE_ALL -> true
        }
        if (!hasAccess) {
            return AccessResult.Failure
        }
        return AccessResult.Success(
            tasksRepository.add(
                name,
                description,
                dueDateTime,
                repeatValue,
                repeatUnit,
                repeatEndDateTime,
                houseId,
                memberId,
                rotateMember,
                createdDate,
                status
            )
        )
    }

    /**
     * Returns [AccessResult.Success] with false data if the task doesn't exist or could not be edited
     */
    suspend fun edit(
        requesterMemberId: String,
        taskId: String,
        name: String,
        description: String,
        dueDateTime: LocalDateTime,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        repeatEndDateTime: LocalDateTime?,
        rotateMember: Boolean,
        status: ActiveStatus,
        shouldDeleteTaskAssignments: Boolean
    ): AccessResult<Boolean> {
        val existingTask = tasksRepository.get(taskId) ?: return AccessResult.Success(false)
        if (!canWrite(requesterMemberId, existingTask)) {
            return AccessResult.Failure
        }
        val result = if (shouldDeleteTaskAssignments) {
            tasksTaskAssignmentsRepository.edit(
                taskId,
                name,
                description,
                dueDateTime,
                repeatValue,
                repeatUnit,
                repeatEndDateTime,
                rotateMember,
                status
            )
        } else {
            tasksRepository.edit(
                taskId,
                name,
                description,
                dueDateTime,
                repeatValue,
                repeatUnit,
                repeatEndDateTime,
                rotateMember,
                status
            )
        }
        return AccessResult.Success(result)
    }

    /**
     * Returns [AccessResult.Success] with false data if the task doesn't exist or could not be deleted
     */
    suspend fun delete(requesterMemberId: String, taskId: String): AccessResult<Boolean> {
        val existingTask = tasksRepository.get(taskId) ?: return AccessResult.Success(false)
        if (!canWrite(requesterMemberId, existingTask)) {
            return AccessResult.Failure
        }
        return AccessResult.Success(tasksTaskAssignmentsRepository.delete(taskId))
    }

    private suspend fun canWrite(requesterMemberId: String, task: Task): Boolean {
        return when (getAccess(requesterMemberId)) {
            Access.NONE -> false
            Access.READ_HOUSE_WRITE_OWN -> {
                task.memberId == requesterMemberId && task.houseId in getHouseIds(requesterMemberId)
            }
            Access.READ_HOUSE_WRITE_HOUSE -> task.houseId in getHouseIds(requesterMemberId)
            Access.READ_ALL_WRITE_ALL -> true
        }
    }

    private suspend fun getHouseIds(requesterMemberId: String): List<String> {
        return memberAssignmentsRepository.getForMember(requesterMemberId).map { it.houseId }
    }
}
