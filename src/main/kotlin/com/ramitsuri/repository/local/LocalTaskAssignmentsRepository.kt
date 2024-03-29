package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.TaskAssignments
import com.ramitsuri.events.Event
import com.ramitsuri.events.EventService
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.models.TaskAssignmentDto
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import com.ramitsuri.repository.interfaces.TaskAssignmentInsert
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andNot
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
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
    private val uuidConverter: Converter<String, UUID>,
    private val eventService: EventService
) : TaskAssignmentsRepository {
    override suspend fun add(taskAssignments: List<TaskAssignmentInsert>) {
        val insertedIds = mutableListOf<String>()
        taskAssignments.forEach { toInsert ->
            DatabaseFactory.query {
                val id = TaskAssignments.insertAndGetId {
                    it[TaskAssignments.statusType] = toInsert.progressStatus.key
                    it[TaskAssignments.statusDate] = instantConverter.toStorage(toInsert.progressStatusDateTime)
                    it[TaskAssignments.taskId] = uuidConverter.toStorage(toInsert.taskId)
                    it[TaskAssignments.memberId] = uuidConverter.toStorage(toInsert.memberId)
                    it[TaskAssignments.dueDate] = localDateTimeConverter.toStorage(toInsert.dueDateTime)
                    it[TaskAssignments.createdDate] = instantConverter.toStorage(toInsert.createdDateTime)
                    it[TaskAssignments.createType] = toInsert.createType.key
                    it[TaskAssignments.statusByMember] = null
                }.value
                insertedIds.add(uuidConverter.toMain(id))
            }
        }
        eventService.post(Event.AssignmentsAdded(insertedIds))
    }

    override suspend fun edit(taskAssignments: List<TaskAssignmentDto>, requesterMemberId: String): List<String> {
        val updatedIds = mutableListOf<String>()
        DatabaseFactory.query {
            taskAssignments.forEach { taskAssignmentDto ->
                val uuid = uuidConverter.toStorage(taskAssignmentDto.id)
                val requesterMemberIdUuid = uuidConverter.toStorage(requesterMemberId)
                val result = TaskAssignments.update({
                    TaskAssignments.id.eq(uuid)
                        .andNot { TaskAssignments.statusType.eq(taskAssignmentDto.progressStatus) }
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
        eventService.post(Event.AssignmentsUpdated(updatedIds))
        // Returning everything so that clients can update their local state. It's possible that some assignments
        // weren't updated in case they already had the new progress status from a different client. In that case, the
        // assignment does have the new status, just not from the client requesting this change.
        return taskAssignments.map { it.id }
    }

    override suspend fun getMostRecentForTask(taskId: String): TaskAssignment? {
        val taskUuid = uuidConverter.toStorage(taskId)
        val rowValue = DatabaseFactory.query {
            TaskAssignments.select { TaskAssignments.taskId.eq(taskUuid) }
                .orderBy(TaskAssignments.dueDate to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
        } ?: return null
        return rowToTaskAssignmentRawValues(rowValue).toAssignment()
    }

    override suspend fun get(taskAssignmentIds: List<String>): List<TaskAssignment> {
        val taskAssignmentUuids = taskAssignmentIds.map { uuidConverter.toStorage(it) }
        val rowValues = DatabaseFactory.query {
            TaskAssignments.select { TaskAssignments.id.inList(taskAssignmentUuids) }
                .filterNotNull()
                .map { row ->
                    rowToTaskAssignmentRawValues(row)
                }
        }
        return rowValues.toAssignments()
    }

    override suspend fun editOwn(
        taskAssignments: List<TaskAssignmentDto>,
        requesterMemberId: String
    ): List<String> {
        val updatedIds = mutableListOf<String>()
        DatabaseFactory.query {
            taskAssignments.forEach { taskAssignmentDto ->
                val uuid = uuidConverter.toStorage(taskAssignmentDto.id)
                val requesterMemberIdUuid = uuidConverter.toStorage(requesterMemberId)
                val result = TaskAssignments.update({
                    TaskAssignments.id.eq(uuid)
                        .and { TaskAssignments.memberId.eq(requesterMemberIdUuid) }
                        .andNot { TaskAssignments.statusType.eq(taskAssignmentDto.progressStatus) }
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
        eventService.post(Event.AssignmentsUpdated(updatedIds))
        // Returning everything so that clients can update their local state. It's possible that some assignments
        // weren't updated in case they already had the new progress status from a different client. In that case, the
        // assignment does have the new status, just not from the client requesting this change.
        return taskAssignments.map { it.id }
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
                    TaskAssignments.id.eq(uuid)
                        .and { TaskAssignments.taskId.inList(taskIdUuids) }
                        .andNot { TaskAssignments.statusType.eq(taskAssignmentDto.progressStatus) }
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
        eventService.post(Event.AssignmentsUpdated(updatedIds))
        // Returning everything so that clients can update their local state. It's possible that some assignments
        // weren't updated in case they already had the new progress status from a different client. In that case, the
        // assignment does have the new status, just not from the client requesting this change.
        return taskAssignments.map { it.id }
    }

    override suspend fun get(filter: TaskAssignmentFilter): List<TaskAssignment> {
        val rowValues = DatabaseFactory.query {
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
            query.filterNotNull().map { row ->
                rowToTaskAssignmentRawValues(row)
            }
        }
        return rowValues.toAssignments()
    }

    override suspend fun getForHouse(filter: TaskAssignmentFilter, houseIds: List<String>): List<TaskAssignment> {
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
        val rowValues = DatabaseFactory.query {
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

            query.filterNotNull().map { row ->
                rowToTaskAssignmentRawValues(row)
            }
        }
        return rowValues.toAssignments()
    }

    private fun rowToTaskAssignmentRawValues(row: ResultRow): RawValues {
        val statusByMember = row[TaskAssignments.statusByMember]
        val statusByMemberString = if (statusByMember != null) {
            uuidConverter.toMain(statusByMember)
        } else {
            null
        }
        return RawValues(
            id = row[TaskAssignments.id].toString(),
            memberId = uuidConverter.toMain(row[TaskAssignments.memberId]),
            taskId = uuidConverter.toMain(row[TaskAssignments.taskId]),
            statusDate = instantConverter.toMain(row[TaskAssignments.statusDate]),
            progressStatus = ProgressStatus.fromKey(row[TaskAssignments.statusType]),
            dueDate = localDateTimeConverter.toMain(row[TaskAssignments.dueDate]),
            createDate = instantConverter.toMain(row[TaskAssignments.createdDate]),
            createType = CreateType.fromKey(row[TaskAssignments.createType]),
            statusByMember = statusByMemberString
        )
    }

    private suspend fun List<RawValues>.toAssignments(): List<TaskAssignment> {
        return mapNotNull { rawValue ->
            rawValue.toAssignment()
        }
    }

    private suspend fun RawValues.toAssignment(): TaskAssignment? {
        val member = membersRepository.get(memberId)
        val task = tasksRepository.get(taskId)
        return if (member != null && task != null) {
            TaskAssignment(
                id = id,
                progressStatus = progressStatus,
                progressStatusDate = statusDate,
                task = task,
                member = member,
                dueDateTime = dueDate,
                createdDate = createDate,
                createType = createType,
                statusByMember = statusByMember
            )
        } else {
            null
        }
    }

    private data class RawValues(
        val id: String,
        val memberId: String,
        val taskId: String,
        val statusDate: Instant,
        val progressStatus: ProgressStatus,
        val dueDate: LocalDateTime,
        val createDate: Instant,
        val createType: CreateType,
        val statusByMember: String?
    )
}