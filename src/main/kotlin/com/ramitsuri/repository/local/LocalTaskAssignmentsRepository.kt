package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.TaskAssignments
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.CreateType
import com.ramitsuri.models.Member
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.Task
import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.models.TaskAssignmentDto
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class LocalTaskAssignmentsRepository(
    private val tasksRepository: TasksRepository,
    private val membersRepository: MembersRepository,
    private val housesRepository: HousesRepository,
    private val instantConverter: Converter<Instant, String>,
    private val localDateTimeConverter: Converter<LocalDateTime, String>,
    private val uuidConverter: Converter<String, UUID>
) : TaskAssignmentsRepository {
    override suspend fun add(
        progressStatus: ProgressStatus,
        statusDate: Instant,
        taskId: String,
        memberId: String,
        dueDate: LocalDateTime,
        createdDate: Instant,
        createType: CreateType
    ): TaskAssignment? {
        val task = tasksRepository.get(taskId) ?: return null
        val member = membersRepository.get(memberId) ?: return null
        var statement: InsertStatement<Number>? = null
        DatabaseFactory.query {
            statement = TaskAssignments.insert { taskAssignment ->
                taskAssignment[TaskAssignments.statusType] = progressStatus.key
                taskAssignment[TaskAssignments.statusDate] = instantConverter.toStorage(statusDate)
                taskAssignment[TaskAssignments.taskId] = uuidConverter.toStorage(taskId)
                taskAssignment[TaskAssignments.memberId] = uuidConverter.toStorage(memberId)
                taskAssignment[TaskAssignments.dueDate] = localDateTimeConverter.toStorage(dueDate)
                taskAssignment[TaskAssignments.createdDate] = instantConverter.toStorage(createdDate)
                taskAssignment[TaskAssignments.createType] = createType.key
            }
        }
        statement?.resultedValues?.get(0)?.let {
            return rowToTaskAssignment(it, member, task)
        }
        return null
    }

    override suspend fun edit(taskAssignments: List<TaskAssignmentDto>, requesterMemberId: String): List<String> {
        val updatedIds = mutableListOf<String>()
        DatabaseFactory.query {
            taskAssignments.forEach { taskAssignmentDto ->
                val uuid = uuidConverter.toStorage(taskAssignmentDto.id)
                val requesterMemberIdUuid = uuidConverter.toStorage(requesterMemberId)
                val result = TaskAssignments.update({ TaskAssignments.id.eq(uuid) }) {
                    it[statusDate] = instantConverter.toStorage(taskAssignmentDto.progressStatusDate)
                    it[statusType] = taskAssignmentDto.progressStatus
                    it[statusByMember] = requesterMemberIdUuid
                }
                if (result == 1) { // Indicates row updated
                    updatedIds.add(taskAssignmentDto.id)
                }
            }
        }
        return updatedIds
    }

    override suspend fun get(): List<TaskAssignment> {
        val members = membersRepository.get()
        val tasks = tasksRepository.get()
        return DatabaseFactory.query {
            TaskAssignments.selectAll().filterNotNull().mapNotNull { row ->
                val member = members.firstOrNull { it.id == rowToMemberId(row) }
                val task = tasks.firstOrNull { it.id == rowToTaskId(row) }
                if (member != null && task != null) {
                    rowToTaskAssignment(row, member, task)
                } else {
                    null
                }
            }
        }
    }

    override suspend fun editOwn(taskAssignments: List<TaskAssignmentDto>, requesterMemberId: String): List<String> {
        val updatedIds = mutableListOf<String>()
        DatabaseFactory.query {
            taskAssignments.forEach { taskAssignmentDto ->
                val uuid = uuidConverter.toStorage(taskAssignmentDto.id)
                val requesterMemberIdUuid = uuidConverter.toStorage(requesterMemberId)
                val result = TaskAssignments.update({
                    TaskAssignments.id.eq(uuid).and { TaskAssignments.memberId.eq(requesterMemberIdUuid) }
                }) {
                    it[statusDate] = instantConverter.toStorage(taskAssignmentDto.progressStatusDate)
                    it[statusType] = taskAssignmentDto.progressStatus
                    it[statusByMember] = requesterMemberIdUuid
                }
                if (result == 1) { // Indicates row updated
                    updatedIds.add(taskAssignmentDto.id)
                }
            }
        }
        return updatedIds
    }

    override suspend fun editForHouse(
        taskAssignments: List<TaskAssignmentDto>,
        houseIds: List<String>,
        requesterMemberId: String
    ): List<String> {
        val taskIdUuids = tasksRepository.getForHouses(houseIds).map { uuidConverter.toStorage(it.id) }
        val updatedIds = mutableListOf<String>()
        DatabaseFactory.query {
            taskAssignments.forEach { taskAssignmentDto ->
                val uuid = uuidConverter.toStorage(taskAssignmentDto.id)
                val requesterMemberIdUuid = uuidConverter.toStorage(requesterMemberId)
                val result = TaskAssignments.update({
                    TaskAssignments.id.eq(uuid).and { TaskAssignments.taskId.inList(taskIdUuids) }
                }) {
                    it[statusDate] = instantConverter.toStorage(taskAssignmentDto.progressStatusDate)
                    it[statusType] = taskAssignmentDto.progressStatus
                    it[statusByMember] = requesterMemberIdUuid
                }
                if (result == 1) { // Indicates row updated
                    updatedIds.add(taskAssignmentDto.id)
                }
            }
        }
        return updatedIds
    }

    override suspend fun get(filter: TaskAssignmentFilter): List<TaskAssignment> {
        val members = membersRepository.get()
        val houses = housesRepository.get()
        val houseIdsFiltered = if (filter.onlyActiveAndPausedHouse) {
            houses.filter { it.status == ActiveStatus.ACTIVE || it.status == ActiveStatus.PAUSED }
                .map { it.id }
        } else {
            houses.map { it.id }
        }
        val tasks = tasksRepository.getForHouses(houseIdsFiltered)
        return DatabaseFactory.query {
            var query = TaskAssignments
                .selectAll()
            filter.memberId?.let {
                val condition = TaskAssignments.memberId eq uuidConverter.toStorage(it)
                query = query.andWhere { condition }
            }
            if (filter.notMemberId != null && filter.notMemberId != filter.memberId) {
                val condition = TaskAssignments.memberId neq uuidConverter.toStorage(filter.notMemberId)
                query = query.andWhere { condition }
            }
            if (filter.progressStatus != ProgressStatus.UNKNOWN) {
                val condition = TaskAssignments.statusType eq filter.progressStatus.key
                query = query.andWhere { condition }
            }
            query.filterNotNull().mapNotNull { row ->
                val member = members.firstOrNull { it.id == rowToMemberId(row) }
                val task = tasks.firstOrNull { it.id == rowToTaskId(row) }
                if (member != null && task != null) {
                    rowToTaskAssignment(row, member, task)
                } else {
                    null
                }
            }
        }
    }

    override suspend fun getForHouse(filter: TaskAssignmentFilter, houseIds: List<String>): List<TaskAssignment> {
        val members = membersRepository.get()
        val houses = housesRepository.get()
        val houseIdsFiltered = if (filter.onlyActiveAndPausedHouse) {
            houses.filter { houseIds.contains(it.id) }
                .filter { it.status == ActiveStatus.ACTIVE || it.status == ActiveStatus.PAUSED }
                .map { it.id }
        } else {
            houseIds
        }
        // Get tasks that belong to the houses provided
        val tasks = tasksRepository.getForHouses(houseIdsFiltered)
        val taskIdUuids = tasks.map { uuidConverter.toStorage(it.id) }
        return DatabaseFactory.query {
            var query = TaskAssignments
                .selectAll()
            filter.memberId?.let {
                val condition = TaskAssignments.memberId eq uuidConverter.toStorage(it)
                query = query.andWhere { condition }
            }
            if (filter.notMemberId != null && filter.notMemberId != filter.memberId) {
                val condition = TaskAssignments.memberId neq uuidConverter.toStorage(filter.notMemberId)
                query = query.andWhere { condition }
            }
            if (filter.progressStatus != ProgressStatus.UNKNOWN) {
                val condition = TaskAssignments.statusType eq filter.progressStatus.key
                query = query.andWhere { condition }
            }
            // Filter assignments whose tasks belong to the houses provided
            val condition = TaskAssignments.taskId inList taskIdUuids
            query = query.andWhere { condition }

            query.filterNotNull().mapNotNull { row ->
                val member = members.firstOrNull { it.id == rowToMemberId(row) }
                val task = tasks.firstOrNull { it.id == rowToTaskId(row) }
                if (member != null && task != null) {
                    rowToTaskAssignment(row, member, task)
                } else {
                    null
                }
            }
        }
    }

    private fun rowToTaskAssignment(row: ResultRow, member: Member, task: Task): TaskAssignment {
        val id = row[TaskAssignments.id].toString()
        val statusDate = instantConverter.toMain(row[TaskAssignments.statusDate])
        val progressStatus = ProgressStatus.fromKey(row[TaskAssignments.statusType])
        val dueDate = localDateTimeConverter.toMain(row[TaskAssignments.dueDate])
        val createdDate = instantConverter.toMain(row[TaskAssignments.createdDate])
        val createType = CreateType.fromKey(row[TaskAssignments.createType])
        val statusByMember = row[TaskAssignments.statusByMember]
        val statusByMemberString = if (statusByMember != null) {
            uuidConverter.toMain(statusByMember)
        } else {
            null
        }
        return TaskAssignment(
            id,
            progressStatus,
            statusDate,
            task,
            member,
            dueDate,
            createdDate,
            createType,
            statusByMemberString
        )
    }

    private fun rowToMemberId(row: ResultRow): String {
        return uuidConverter.toMain(row[TaskAssignments.memberId])
    }

    private fun rowToTaskId(row: ResultRow): String {
        return uuidConverter.toMain(row[TaskAssignments.taskId])
    }
}