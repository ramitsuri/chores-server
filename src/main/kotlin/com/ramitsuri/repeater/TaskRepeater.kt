package com.ramitsuri.repeater

import com.ramitsuri.events.Event
import com.ramitsuri.events.EventService
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.extensions.isLaterThan
import com.ramitsuri.models.*
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class TaskRepeater(
    private val eventService: EventService,
    private val tasksRepository: TasksRepository,
    private val membersRepository: MembersRepository,
    private val memberAssignmentsRepository: MemberAssignmentsRepository,
    private val taskAssignmentsRepository: TaskAssignmentsRepository,
    private val dispatcher: CoroutineDispatcher
): Loggable {
    override val log = logger()

    suspend fun start(
        runDateTime: ZonedDateTime,
        zoneId: ZoneId
    ) {
        val newAssignments = getNewAssignments(
            tasksRepository.get(),
            membersRepository.get(),
            taskAssignmentsRepository.get(),
            runDateTime,
            zoneId
        )
        log.info("Adding ${newAssignments.size} new assignments")
        for (newAssignment in newAssignments) {
            taskAssignmentsRepository.add(
                progressStatus = newAssignment.progressStatus,
                statusDate = newAssignment.progressStatusDate,
                taskId = newAssignment.task.id,
                memberId = newAssignment.member.id,
                createdDate = newAssignment.createdDate,
                createType = newAssignment.createType,
                dueDate = newAssignment.dueDateTime
            )
        }
        if (newAssignments.isNotEmpty()) {
            eventService.post(Event.AssignmentsAdded(newAssignments))
        }
    }

    /**
     * This method will create new assignments for tasks if they can be created based on due date time for the
     * most recent assignment for a task was created and what repeat schedule does the task is set up with.
     */
    private suspend fun getNewAssignments(
        tasks: List<Task>,
        members: List<Member>,
        taskAssignments: List<TaskAssignment>,
        runDateTime: ZonedDateTime,
        zoneId: ZoneId
    ): List<TaskAssignment> {
        return withContext(dispatcher) {
            val assignmentsToAdd = mutableListOf<TaskAssignment>()
            for (task in tasks) {
                val mostRecentAssignment = taskAssignments
                    .filter {it.task.id == task.id}
                    .maxByOrNull {it.dueDateTime}
                val taskMember = members.firstOrNull {it.id == task.memberId}
                val newAssignment = if (taskMember == null) {
                    null
                } else if (mostRecentAssignment == null) { // First ever assignment
                    getFirstAssignment(task, taskMember, runDateTime)
                } else {
                    getRepeatAssignment(
                        task,
                        taskMember,
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

    private fun getFirstAssignment(task: Task, member: Member, runDateTime: ZonedDateTime): TaskAssignment {
        return TaskAssignment(
            id = "",
            progressStatus = ProgressStatus.TODO,
            progressStatusDate = runDateTime.toInstant(),
            task = task,
            member = member,
            createdDate = runDateTime.toInstant(),
            createType = CreateType.AUTO,
            dueDateTime = task.dueDateTime
        )
    }

    private suspend fun getRepeatAssignment(
        task: Task,
        taskMember: Member,
        runDateTime: ZonedDateTime,
        mostRecentAssignment: TaskAssignment,
        zoneId: ZoneId
    ): TaskAssignment? {
        val mostRecentDueDateTime = getZonedDateTime(mostRecentAssignment.dueDateTime, zoneId)
        val mostRecentMemberId = mostRecentAssignment.member.id
        val newDateTime =
            getNewTime(task.repeatValue, task.repeatUnit, mostRecentDueDateTime, runDateTime) ?: return null

        if (!canRun(task, mostRecentAssignment.progressStatus, mostRecentDueDateTime, runDateTime)) {
            return null
        }
        val newAssignmentMember = if (task.rotateMember) {
            getNewAssignmentMemberId(task.houseId, mostRecentMemberId)
        } else {
            taskMember
        }

        val dueDateTime = newDateTime
            .withNano(0)
        return TaskAssignment(
            id = "",
            progressStatus = ProgressStatus.TODO,
            progressStatusDate = runDateTime.toInstant(),
            task = task,
            member = newAssignmentMember,
            createdDate = runDateTime.toInstant(),
            createType = CreateType.AUTO,
            dueDateTime = dueDateTime.toInstant()
        )
    }

    private fun getNewTime(
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        mostRecentDueDateTime: ZonedDateTime,
        runDateTime: ZonedDateTime
    ): ZonedDateTime? {
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
                runDateTime
            }
            RepeatUnit.NONE -> {
                null
            }
        }
    }

    private suspend fun getNewAssignmentMemberId(houseId: String, mostRecentAssignmentMemberId: String): Member {
        val membersOfHouse = memberAssignmentsRepository.getForHouse(houseId).sortedBy {it.member.name}
        var indexOfNewMember = 0
        for ((index, member) in membersOfHouse.withIndex()) {
            if (member.member.id == mostRecentAssignmentMemberId) {
                indexOfNewMember = if (index + 1 >= membersOfHouse.size) 0 else (index + 1)
                break
            }
        }
        return membersOfHouse[indexOfNewMember].member
    }

    private fun getZonedDateTime(instant: Instant, zoneId: ZoneId): ZonedDateTime {
        return ZonedDateTime.ofInstant(instant, zoneId)
    }

    private fun canRun(
        task: Task,
        mostRecentProgressStatus: ProgressStatus,
        mostRecentDueDateTime: ZonedDateTime,
        runDateTime: ZonedDateTime
    ): Boolean {
        // New assignment for a task can only be created if it's of repeat type OnComplete and its most recent assignment
        // was complete, or it's regular repeating type and run date time is later than most recent due date time.
        val isTaskRepeatOnComplete = task.repeatUnit == RepeatUnit.ON_COMPLETE
        return if (isTaskRepeatOnComplete) {
            mostRecentProgressStatus == ProgressStatus.DONE
        } else {
            runDateTime.isLaterThan(mostRecentDueDateTime)
        }
    }
}