package com.ramitsuri.repository.local

import com.ramitsuri.data.*
import com.ramitsuri.models.*
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.time.Instant
import java.util.*

class LocalTaskAssignmentsRepository(
    private val tasksRepository: TasksRepository,
    private val membersRepository: MembersRepository,
    private val instantConverter: Converter<Instant, String>,
    private val uuidConverter: Converter<String, UUID>
): TaskAssignmentsRepository {
    override suspend fun add(
        progressStatus: ProgressStatus,
        statusDate: Instant,
        taskId: String,
        memberId: String,
        dueDate: Instant,
        createdDate: Instant,
        createType: CreateType
    ): TaskAssignment? {
        val task = tasksRepository.get(taskId) ?: return null
        val member = membersRepository.get(memberId) ?: return null
        var statement: InsertStatement<Number>? = null
        DatabaseFactory.query {
            statement = TaskAssignments.insert {taskAssignment ->
                taskAssignment[TaskAssignments.statusType] = progressStatus.key
                taskAssignment[TaskAssignments.statusDate] = instantConverter.toStorage(statusDate)
                taskAssignment[TaskAssignments.taskId] = uuidConverter.toStorage(taskId)
                taskAssignment[TaskAssignments.memberId] = uuidConverter.toStorage(memberId)
                taskAssignment[TaskAssignments.dueDate] = instantConverter.toStorage(dueDate)
                taskAssignment[TaskAssignments.createdDate] = instantConverter.toStorage(createdDate)
                taskAssignment[TaskAssignments.createType] = createType.key
            }
        }
        statement?.resultedValues?.get(0)?.let {
            return rowToTaskAssignment(it, member, task)
        }
        return null
    }

    override suspend fun delete(): Int {
        return DatabaseFactory.query {
            TaskAssignments.deleteAll()
        }
    }

    override suspend fun delete(id: String): Int {
        return DatabaseFactory.query {
            val uuid = uuidConverter.toStorage(id)
            TaskAssignments.deleteWhere {TaskAssignments.id.eq(uuid)}
        }
    }

    override suspend fun edit(id: String, progressStatus: ProgressStatus, statusDate: Instant): TaskAssignment? {
        DatabaseFactory.query {
            val uuid = uuidConverter.toStorage(id)
            TaskAssignments.update({TaskAssignments.id.eq(uuid)}) {taskAssignment ->
                taskAssignment[TaskAssignments.statusDate] = instantConverter.toStorage(statusDate)
                taskAssignment[TaskAssignments.statusType] = progressStatus.key
            }
        }
        return get(id)
    }

    override suspend fun get(): List<TaskAssignment> {
        val members = membersRepository.get()
        val tasks = tasksRepository.get()
        return DatabaseFactory.query {
            TaskAssignments.selectAll().filterNotNull().mapNotNull {row ->
                val member = members.firstOrNull {it.id == rowToMemberId(row)}
                val task = tasks.firstOrNull {it.id == rowToTaskId(row)}
                if (member != null && task != null) {
                    rowToTaskAssignment(row, member, task)
                } else {
                    null
                }
            }
        }
    }

    override suspend fun get(id: String): TaskAssignment? {
        val resultRow = DatabaseFactory.query {
            val uuid = uuidConverter.toStorage(id)
            TaskAssignments.select {TaskAssignments.id.eq(uuid)}.singleOrNull()
        } ?: return null
        val member = membersRepository.get(rowToMemberId(resultRow)) ?: return null
        val task = tasksRepository.get(rowToTaskId(resultRow)) ?: return null
        return rowToTaskAssignment(resultRow, member, task)
    }

    private fun rowToTaskAssignment(row: ResultRow, member: Member, task: Task): TaskAssignment {
        val id = row[TaskAssignments.id].toString()
        val statusDate = instantConverter.toMain(row[TaskAssignments.statusDate])
        val progressStatus = ProgressStatus.fromKey(row[TaskAssignments.statusType])
        val dueDate = instantConverter.toMain(row[TaskAssignments.dueDate])
        val createdDate = instantConverter.toMain(row[TaskAssignments.createdDate])
        val createType = CreateType.fromKey(row[TaskAssignments.createType])
        return TaskAssignment(id, progressStatus, statusDate, task, member, dueDate, createdDate, createType)
    }

    private fun rowToMemberId(row: ResultRow): String {
        return uuidConverter.toMain(row[TaskAssignments.memberId])
    }

    private fun rowToTaskId(row: ResultRow): String {
        return uuidConverter.toMain(row[TaskAssignments.taskId])
    }
}