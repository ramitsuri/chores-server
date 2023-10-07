package com.ramitsuri.repeater

import com.ramitsuri.extensions.Loggable
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.CreateType
import com.ramitsuri.models.House
import com.ramitsuri.models.Member
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentInsert
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TaskRepeater(
    private val tasksRepository: TasksRepository,
    private val membersRepository: MembersRepository,
    private val housesRepository: HousesRepository,
    private val memberAssignmentsRepository: MemberAssignmentsRepository,
    private val taskAssignmentsRepository: TaskAssignmentsRepository,
    private val dispatcher: CoroutineDispatcher
) : Loggable {
    override val log = logger()

    suspend fun add(
        runDateTime: ZonedDateTime,
        zoneId: ZoneId
    ) {
        val newAssignments = getNewAssignments(
            tasksRepository.get(),
            membersRepository.get(),
            housesRepository.get(),
            runDateTime,
            zoneId
        )
        log.info("Adding ${newAssignments.size} new assignments")
        taskAssignmentsRepository.add(newAssignments.map { TaskAssignmentInsert(it) })
    }

    /**
     * This method will create new assignments for tasks if they can be created based on due date time for the
     * most recent assignment for a task was created and what repeat schedule does the task is set up with.
     */
    private suspend fun getNewAssignments(
        tasks: List<Task>,
        members: List<Member>,
        houses: List<House>,
        runDateTime: ZonedDateTime,
        zoneId: ZoneId
    ): List<TaskAssignment> {
        return withContext(dispatcher) {
            val assignmentsToAdd = mutableListOf<TaskAssignment>()
            for (task in tasks) {
                if (task.status != ActiveStatus.ACTIVE) {
                    // Not adding assignments for a task if no longer active
                    continue
                }
                val house = houses.firstOrNull { it.id == task.houseId }
                if (house == null || house.status == ActiveStatus.INACTIVE || house.status == ActiveStatus.UNKNOWN) {
                    // Not adding assignments for houses that are no longer active or status is unknown
                    continue
                }
                val mostRecentAssignment = taskAssignmentsRepository.getMostRecentForTask(taskId = task.id)
                val taskMember = members.firstOrNull { it.id == task.memberId }
                val newAssignment = if (taskMember == null) {
                    null
                } else if (mostRecentAssignment == null) { // First ever assignment
                    getFirstAssignment(task, taskMember, house, runDateTime)
                } else {
                    getRepeatAssignment(
                        task,
                        taskMember,
                        house,
                        runDateTime,
                        mostRecentAssignment,
                        zoneId
                    )
                }
                if (newAssignment != null) {
                    assignmentsToAdd.add(newAssignment)
                }
            }
            assignmentsToAdd
        }
    }

    private fun getFirstAssignment(
        task: Task,
        member: Member,
        house: House,
        runDateTime: ZonedDateTime
    ): TaskAssignment? {
        val progressStatus = if (house.status == ActiveStatus.PAUSED) {
            ProgressStatus.WONT_DO
        } else {
            ProgressStatus.TODO
        }
        val dueDateTime = task.dueDateTime
        if (task.repeatEndDateTime != null && dueDateTime.isAfter(task.repeatEndDateTime)) {
            return null
        }
        return TaskAssignment(
            id = "",
            progressStatus = progressStatus,
            progressStatusDate = runDateTime.toInstant(),
            task = task,
            member = member,
            createdDate = runDateTime.toInstant(),
            createType = CreateType.AUTO,
            dueDateTime = dueDateTime,
            statusByMember = null
        )
    }

    private suspend fun getRepeatAssignment(
        task: Task,
        taskMember: Member,
        house: House,
        runDateTime: ZonedDateTime,
        mostRecentAssignment: TaskAssignment,
        zoneId: ZoneId
    ): TaskAssignment? {
        val mostRecentDueDateTime = mostRecentAssignment.dueDateTime
        val mostRecentMemberId = mostRecentAssignment.member.id
        val newAssignmentDueDateTime =
            getNewAssignmentDueDateTime(task.repeatValue, task.repeatUnit, mostRecentDueDateTime, runDateTime)
                ?: return null

        if (!canAddNewAssignment(
                task,
                mostRecentAssignment.progressStatus,
                newAssignmentDueDateTime,
                runDateTime,
                zoneId
            )
        ) {
            return null
        }

        val newAssignmentMember = if (task.rotateMember) {
            getNewAssignmentMemberId(task.houseId, mostRecentMemberId)
        } else {
            taskMember
        }

        val dueDateTime = newAssignmentDueDateTime
            .withNano(0)
        val progressStatus = if (house.status == ActiveStatus.PAUSED) {
            ProgressStatus.WONT_DO
        } else {
            ProgressStatus.TODO
        }
        return TaskAssignment(
            id = "",
            progressStatus = progressStatus,
            progressStatusDate = runDateTime.toInstant(),
            task = task,
            member = newAssignmentMember,
            createdDate = runDateTime.toInstant(),
            createType = CreateType.AUTO,
            dueDateTime = dueDateTime,
            statusByMember = null
        )
    }

    private fun getNewAssignmentDueDateTime(
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        mostRecentDueDateTime: LocalDateTime,
        runDateTime: ZonedDateTime
    ): LocalDateTime? {
        val repeatLong = repeatValue.toLong()
        return when (repeatUnit) {
            RepeatUnit.HOUR -> {
                mostRecentDueDateTime.plusHours(repeatLong)
            }

            RepeatUnit.DAY -> {
                mostRecentDueDateTime.plusDays(repeatLong)
            }

            RepeatUnit.WEEK -> {
                mostRecentDueDateTime.plusWeeks(repeatLong)
            }

            RepeatUnit.MONTH -> {
                mostRecentDueDateTime.plusMonths(repeatLong)
            }

            RepeatUnit.YEAR -> {
                mostRecentDueDateTime.plusYears(repeatLong)
            }

            RepeatUnit.ON_COMPLETE -> {
                runDateTime.toLocalDateTime()
            }

            RepeatUnit.NONE -> {
                null
            }
        }
    }

    private suspend fun getNewAssignmentMemberId(houseId: String, mostRecentAssignmentMemberId: String): Member {
        val membersOfHouse = memberAssignmentsRepository.getForHouse(houseId).sortedBy { it.member.name }
        var indexOfNewMember = 0
        for ((index, member) in membersOfHouse.withIndex()) {
            if (member.member.id == mostRecentAssignmentMemberId) {
                indexOfNewMember = if (index + 1 >= membersOfHouse.size) 0 else (index + 1)
                break
            }
        }
        return membersOfHouse[indexOfNewMember].member
    }

    private fun canAddNewAssignment(
        task: Task,
        mostRecentProgressStatus: ProgressStatus,
        newAssignmentDueDateTime: LocalDateTime,
        runDateTime: ZonedDateTime,
        timeZone: ZoneId
    ): Boolean {
        if (task.repeatEndDateTime != null && newAssignmentDueDateTime.isAfter(task.repeatEndDateTime)) {
            return false
        }
        // New assignment for a task can only be created if
        // - it's of repeat type OnComplete and its most recent assignment was complete
        // - it's regular repeating type and new due date time is before run date time (now)
        // - it's regular repeating type and new due date time is less than equal to 7 days of run date time (now) in future
        if (task.repeatUnit == RepeatUnit.ON_COMPLETE) {
            return mostRecentProgressStatus == ProgressStatus.DONE
        }
        // Add assignment if somehow it's due date time is in the past. Would happen if app stopped running
        val newAssignmentDueDateTimeZoned = newAssignmentDueDateTime.atZone(timeZone)
        if (newAssignmentDueDateTimeZoned <= runDateTime) {
            return true
        }
        return Duration.between(runDateTime, newAssignmentDueDateTimeZoned) <= Duration.ofDays(7)
    }


}