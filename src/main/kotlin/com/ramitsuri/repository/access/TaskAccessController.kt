package com.ramitsuri.repository.access

import com.ramitsuri.data.LocalDateTimeConverter
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.models.Access
import com.ramitsuri.models.AccessResult
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.AddEditTaskDto
import com.ramitsuri.models.AddEditTaskResultDto
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
    private val localDateTimeConverter: LocalDateTimeConverter,
    membersRepository: MembersRepository,
) : AccessController(membersRepository), Loggable {
    override val log = logger()

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
     * Returns result containing list of client request ids that successfully completed, list of task ids whose task assignments were deleted, and updated tasks
     */
    suspend fun addOrEdit(
        requesterMemberId: String,
        requests: List<AddEditTaskDto>,
    ): AddEditTaskResultDto {
        val successfulRequestIds = mutableListOf<String>()
        val deleteTaskAssignmentsTaskIds = mutableListOf<String>()
        val tasks = mutableListOf<Task>()

        requests.forEach { addOrEdit ->
            when (addOrEdit) {
                is AddEditTaskDto.Add -> {
                    val name = addOrEdit.name
                    val description = addOrEdit.description
                    val dueDateTime =
                        if (addOrEdit.dueDateTime != null) localDateTimeConverter.toMain(addOrEdit.dueDateTime) else null
                    val repeatValue = addOrEdit.repeatValue ?: 0
                    val repeatUnit = RepeatUnit.fromKey(addOrEdit.repeatUnit ?: RepeatUnit.NONE.key)
                    val repeatEndDateTime = if (addOrEdit.repeatEndDateTime != null) {
                        localDateTimeConverter.toMain(addOrEdit.repeatEndDateTime)
                    } else {
                        null
                    }
                    val houseId = addOrEdit.houseId
                    val memberId = addOrEdit.memberId
                    val rotateMember = addOrEdit.rotateMember ?: false
                    val createdDate = addOrEdit.createdDate
                    if (name != null && description != null && houseId != null && dueDateTime != null && memberId != null) {
                        val result = add(
                            requesterMemberId = requesterMemberId,
                            name = name,
                            description = description,
                            dueDateTime = dueDateTime,
                            repeatValue = repeatValue,
                            repeatUnit = repeatUnit,
                            repeatEndDateTime = repeatEndDateTime,
                            houseId = houseId,
                            memberId = memberId,
                            rotateMember = rotateMember,
                            createdDate = createdDate,
                            status = ActiveStatus.ACTIVE,
                        )
                        if (result is AccessResult.Success && result.data != null) {
                            successfulRequestIds.add(addOrEdit.clientRequestId)
                            tasks.add(result.data)
                        } else {
                            log.warning("Add access requirements not met or error occurred")
                        }
                    } else {
                        val message = buildString {
                            append("null values: ")
                            if (name == null) append("name ")
                            if (description == null) append("description ")
                            if (houseId == null) append("houseId ")
                            if (dueDateTime == null) append("dueDateTime ")
                            if (memberId == null) append("memberId ")
                        }
                        log.warning("Add requirements not met, $message")
                    }
                }

                is AddEditTaskDto.Edit -> {
                    val existingTaskResult = get(requesterMemberId, addOrEdit.taskId)
                    val existingTask = when (existingTaskResult) {
                        is AccessResult.Failure -> {
                            log.warning("Task access denied for edit: taskId=${addOrEdit.taskId}")
                            null
                        }

                        is AccessResult.Success<Task?> -> {
                            existingTaskResult.data
                        }
                    }
                    if (existingTask == null) {
                        log.warning("Task not found edit: taskId=${addOrEdit.taskId}")
                        return@forEach
                    }

                    val name = addOrEdit.name ?: existingTask.name
                    val description = addOrEdit.description ?: existingTask.description
                    val dueDateTime = addOrEdit.dueDateTime
                        ?.let { localDateTimeConverter.toMain(it) }
                        ?: existingTask.dueDateTime
                    val repeatUnit = addOrEdit.repeatUnit
                        ?.let { RepeatUnit.fromKey(it) }
                        ?: existingTask.repeatUnit
                    val repeatValue = addOrEdit.repeatValue ?: existingTask.repeatValue
                    val repeatEndDateTime = addOrEdit.repeatEndDateTime
                        ?.let { localDateTimeConverter.toMain(it) }
                        ?: existingTask.repeatEndDateTime
                    val rotateMember = addOrEdit.rotateMember ?: existingTask.rotateMember
                    val status = addOrEdit.status?.let { ActiveStatus.fromKey(it) } ?: existingTask.status
                    val result = edit(
                        requesterMemberId = requesterMemberId,
                        taskId = addOrEdit.taskId,
                        name = name,
                        description = description,
                        dueDateTime = dueDateTime,
                        repeatValue = repeatValue,
                        repeatUnit = repeatUnit,
                        repeatEndDateTime = repeatEndDateTime,
                        rotateMember = rotateMember,
                        status = status,
                        shouldDeleteTaskAssignments = addOrEdit.shouldDeleteTaskAssignments
                    )
                    if (result is AccessResult.Success && result.data != null) {
                        successfulRequestIds.add(addOrEdit.clientRequestId)
                        tasks.add(result.data)
                        if (addOrEdit.shouldDeleteTaskAssignments) {
                            deleteTaskAssignmentsTaskIds.add(addOrEdit.taskId)
                        }
                    } else {
                        log.warning("Edit access requirements not met or edit failed for clientRequestId=${addOrEdit.clientRequestId}")
                    }
                }
            }
        }
        return AddEditTaskResultDto(
            successfulRequestIds = successfulRequestIds,
            deleteTaskAssignmentsTaskIds = deleteTaskAssignmentsTaskIds,
            tasks = tasks
        )
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
        status: ActiveStatus,
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
     * Returns [AccessResult.Success] with null data if the task doesn't exist or could not be edited
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
        shouldDeleteTaskAssignments: Boolean,
    ): AccessResult<Task?> {
        val existingTask = tasksRepository.get(taskId) ?: return AccessResult.Success(null)
        if (!canWrite(requesterMemberId, existingTask)) {
            return AccessResult.Failure
        }
        val success = if (shouldDeleteTaskAssignments) {
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
        if (!success) {
            return AccessResult.Success(null)
        }
        val updatedTask = tasksRepository.get(taskId)
        return AccessResult.Success(updatedTask)
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
