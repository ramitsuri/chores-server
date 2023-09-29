package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory.query
import com.ramitsuri.data.Tasks
import com.ramitsuri.events.Event
import com.ramitsuri.events.EventService
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class LocalTasksRepository(
    private val housesRepository: HousesRepository,
    private val instantConverter: Converter<Instant, String>,
    private val localDateTimeConverter: Converter<LocalDateTime, String>,
    private val uuidConverter: Converter<String, UUID>,
    private val eventService: EventService
) : TasksRepository, Loggable {
    override val log = logger()
    override suspend fun add(
        name: String,
        description: String,
        dueDate: LocalDateTime,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        repeatEndDate: LocalDateTime?,
        houseId: String,
        memberId: String,
        rotateMember: Boolean,
        createdDate: Instant,
        status: ActiveStatus
    ): Task? {
        if (housesRepository.get(houseId) == null) {
            log.warning("HouseId doesn't belong to a house")
            return null
        }
        var statement: InsertStatement<Number>? = null
        query {
            statement = Tasks.insert { task ->
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
                task[Tasks.houseId] = uuidConverter.toStorage(houseId)
                task[Tasks.memberId] = uuidConverter.toStorage(memberId)
                task[Tasks.rotateMember] = rotateMember
                task[Tasks.createdDate] = instantConverter.toStorage(createdDate)
                task[Tasks.activeStatus] = status.key
            }
        }
        statement?.resultedValues?.get(0)?.let {
            return rowToTask(it)
        } ?: run {
            log.warning("Cannot convert row to Task")
            return null
        }
    }

    // Internal only
    override suspend fun delete(): Int {
        return query {
            Tasks.deleteAll()
        }
    }

    override suspend fun edit(
        id: String,
        name: String,
        description: String,
        dueDate: LocalDateTime,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        repeatEndDate: LocalDateTime?,
        rotateMember: Boolean,
        status: ActiveStatus
    ): Boolean {
        val edited = query {
            val uuid = uuidConverter.toStorage(id)
            Tasks.update({ Tasks.id.eq(uuid) }) { task ->
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
                task[Tasks.activeStatus] = status.key
            } > 0
        }
        if (edited) {
            eventService.post(Event.TaskEdited(taskId = id))
        }
        return edited
    }

    override suspend fun get(): List<Task> {
        return query {
            Tasks.selectAll().filterNotNull().map {
                rowToTask(it)
            }
        }
    }

    override suspend fun getForHouses(houseIds: List<String>): List<Task> {
        val houseIdUuids = houseIds.map { uuidConverter.toStorage(it) }
        return query {
            Tasks.select { Tasks.houseId.inList(houseIdUuids) }.filterNotNull().map {
                rowToTask(it)
            }
        }
    }

    override suspend fun get(id: String): Task? {
        return query {
            val uuid = uuidConverter.toStorage(id)
            Tasks.select { Tasks.id.eq(uuid) }.map {
                rowToTask(it)
            }.singleOrNull()
        }
    }

    override suspend fun get(ids: List<String>): List<Task> {
        val taskUuids = ids.map { uuidConverter.toStorage(it) }
        return query {
            Tasks.select { Tasks.id.inList(taskUuids) }
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