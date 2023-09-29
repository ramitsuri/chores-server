package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.TaskAssignments
import com.ramitsuri.data.Tasks
import com.ramitsuri.events.Event
import com.ramitsuri.events.EventService
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.repository.interfaces.TasksTaskAssignmentsRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID

class LocalTasksTaskAssignmentsRepository(
    private val localDateTimeConverter: Converter<LocalDateTime, String>,
    private val uuidConverter: Converter<String, UUID>,
    private val eventService: EventService
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
        status: ActiveStatus
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
            val deleteSuccess = TaskAssignments.deleteWhere {
                TaskAssignments.taskId.eq(uuid)
                    .and(TaskAssignments.statusType.eq(ProgressStatus.TODO.key))
            } > 0
            if (!deleteSuccess) {
                transaction.rollback()
                return@queryWithTransaction false
            }
            return@queryWithTransaction true
        }
        if (success) {
            eventService.post(Event.TaskEdited(taskId))
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
            val deleteAssignmentsSuccess = TaskAssignments.deleteWhere {
                TaskAssignments.taskId.eq(uuid)
                    .and(TaskAssignments.statusType.eq(ProgressStatus.TODO.key))
            } > 0
            if (!deleteAssignmentsSuccess) {
                transaction.rollback()
                return@queryWithTransaction false
            }
            return@queryWithTransaction true
        }
        if (success) {
            eventService.post(Event.TaskEdited(taskId))
        }
        return success
    }
}