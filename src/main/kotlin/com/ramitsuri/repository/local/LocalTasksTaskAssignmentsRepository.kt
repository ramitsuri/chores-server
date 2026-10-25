package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.DatabaseFactory.query
import com.ramitsuri.data.TaskAssignments
import com.ramitsuri.data.Tasks
import com.ramitsuri.events.Event
import com.ramitsuri.events.EventService
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import com.ramitsuri.repository.interfaces.TasksTaskAssignmentsRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class LocalTasksTaskAssignmentsRepository(
    private val localDateTimeConverter: Converter<LocalDateTime, String>,
    private val instantConverter: Converter<Instant, String>,
    private val uuidConverter: Converter<String, UUID>,
    private val eventService: EventService,
    private val clock: Clock,
) : TasksTaskAssignmentsRepository {
    override suspend fun edit(
        taskId: String,
        name: String,
        description: String,
        dueDate: LocalDateTime,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        repeatEndDate: LocalDateTime?,
        rotateMember: Boolean,
        status: ActiveStatus,
    ): Boolean {
        val success = DatabaseFactory.queryWithTransaction { transaction ->
            val uuid = uuidConverter.toStorage(taskId)
            val updateSuccess = Tasks.update({ Tasks.id.eq(uuid) }) { task ->
                task[Tasks.name] = name
                task[Tasks.description] = description
                task[Tasks.dueDate] = localDateTimeConverter.toStorage(dueDate)
                task[Tasks.repeatValue] = repeatValue
                task[Tasks.repeatUnit] = repeatUnit.key
                task[Tasks.repeatEndDate] = if (repeatEndDate == null) {
                    null
                } else {
                    localDateTimeConverter.toStorage(repeatEndDate)
                }
                task[Tasks.rotateMember] = rotateMember
                task[activeStatus] = status.key
            } > 0
            if (!updateSuccess) {
                return@queryWithTransaction false
            }
            // Delete to do assignments for task that is being edited
            TaskAssignments.deleteWhere {
                TaskAssignments.taskId.eq(uuid)
                    .and(TaskAssignments.statusType.eq(ProgressStatus.TODO.key))
            }
            return@queryWithTransaction true
        }
        if (success) {
            eventService.post(
                Event.TaskEdited(taskId),
                Event.TaskNeedsAssignments
            )
        }
        return success
    }

    override suspend fun delete(taskId: String): Boolean {
        val success = DatabaseFactory.queryWithTransaction { transaction ->
            val uuid = uuidConverter.toStorage(taskId)
            val deleteTaskSuccess = Tasks.deleteWhere { Tasks.id.eq(uuid) } > 0
            if (!deleteTaskSuccess) {
                return@queryWithTransaction false
            }
            // Delete to do assignments for task that is being deleted
            TaskAssignments.deleteWhere {
                TaskAssignments.taskId.eq(uuid)
                    .and(TaskAssignments.statusType.eq(ProgressStatus.TODO.key))
            }
            return@queryWithTransaction true
        }
        if (success) {
            eventService.post(Event.TaskEdited(taskId))
        }
        return success
    }

    override suspend fun get(): List<Task> {
        return getInternal()
    }

    override suspend fun getForHouses(houseIds: List<String>): List<Task> {
        return getInternal(houseIds)
    }

    private suspend fun getInternal(houseIds: List<String>? = null): List<Task> {
        fun Query.andWhereHouseIdsMatch() = if (houseIds == null) {
            andWhere { Op.TRUE }
        } else {
            val houseIdUuids = houseIds.map { uuidConverter.toStorage(it) }
            andWhere { Tasks.houseId.inList(houseIdUuids) }
        }
        return query {
            val minDueDate = clock.now().minus(366.days) // Past 1 year
                .toLocalDateTime(TimeZone.UTC)
                .toJavaLocalDateTime()
            val conditionDueDate = TaskAssignments.dueDate greater localDateTimeConverter.toStorage(minDueDate)
            val relevantTaskUuids = TaskAssignments
                .selectAll()
                .where { conditionDueDate }
                .filterNotNull()
                .map { row ->
                    row[TaskAssignments.taskId]
                }
                .toSet()
            if (relevantTaskUuids.isEmpty()) {
                return@query emptyList()
            }
            Tasks
                .selectAll()
                .where { Tasks.id.inList(relevantTaskUuids) }
                .andWhereHouseIdsMatch()
                .filterNotNull()
                .map {
                    rowToTask(it)
                }
        }
    }

    private fun rowToTask(row: ResultRow): Task {
        val id = row[Tasks.id]
        val name = row[Tasks.name]
        val houseId = row[Tasks.houseId]
        val memberId = row[Tasks.memberId]
        val description = row[Tasks.description]
        val dueDate = localDateTimeConverter.toMain(row[Tasks.dueDate])
        val repeatValue = row[Tasks.repeatValue]
        val repeatUnit = RepeatUnit.fromKey(row[Tasks.repeatUnit])
        val repeatEndDateColumnValue = row[Tasks.repeatEndDate]
        val repeatEndDate: LocalDateTime? = if (repeatEndDateColumnValue == null) {
            null
        } else {
            localDateTimeConverter.toMain(repeatEndDateColumnValue)
        }
        val createdDate = instantConverter.toMain(row[Tasks.createdDate])
        val rotateMember = row[Tasks.rotateMember]
        val status = ActiveStatus.fromKey(row[Tasks.activeStatus])
        return Task(
            id = id.toString(),
            name = name,
            description = description,
            dueDateTime = dueDate,
            repeatValue = repeatValue,
            repeatUnit = repeatUnit,
            repeatEndDateTime = repeatEndDate,
            houseId = houseId.toString(),
            memberId = memberId.toString(),
            rotateMember = rotateMember,
            createdDate = createdDate,
            status = status
        )
    }
}
