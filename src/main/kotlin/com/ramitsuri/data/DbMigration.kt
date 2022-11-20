package com.ramitsuri.data

import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class DbMigration(
    private val instantConverter: Converter<Instant, String>,
    private val localDateTimeConverter: Converter<LocalDateTime, String>,
    private val uuidConverter: Converter<String, UUID>
) {
    suspend fun migrateInstantDueDateToLocalDateTime() {
        return DatabaseFactory.query {
            // Tasks table
            val taskIdDueDateTimes = Tasks.selectAll().filterNotNull().map {
                Pair(it[Tasks.id].toString(), instantConverter.toMain(it[Tasks.dueDate]))
            }

            for ((id, dueDateTimeInstant) in taskIdDueDateTimes) {
                val uuid = uuidConverter.toStorage(id)
                val dueDateTimeLocal = dueDateTimeInstant.atZone(ZoneId.of("America/New_York")).toLocalDateTime()
                Tasks.update({ Tasks.id.eq(uuid) }) { task ->
                    task[Tasks.dueDate] = localDateTimeConverter.toStorage(dueDateTimeLocal)
                }
            }

            // Task Assignments table
            val taskAssignmentIdDueDateTimes = TaskAssignments.selectAll().filterNotNull().map {
                Pair(it[TaskAssignments.id].toString(), instantConverter.toMain(it[TaskAssignments.dueDate]))
            }

            for ((id, dueDateTimeInstant) in taskAssignmentIdDueDateTimes) {
                val uuid = uuidConverter.toStorage(id)
                val dueDateTimeLocal = dueDateTimeInstant.atZone(ZoneId.of("America/New_York")).toLocalDateTime()
                TaskAssignments.update({ TaskAssignments.id.eq(uuid) }) { taskAssignment ->
                    taskAssignment[TaskAssignments.dueDate] = localDateTimeConverter.toStorage(dueDateTimeLocal)
                }
            }
        }
    }
}