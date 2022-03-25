package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory.query
import com.ramitsuri.data.Tasks
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.time.Instant
import java.util.*

class LocalTasksRepository(
    private val housesRepository: HousesRepository,
    private val instantConverter: Converter<Instant, String>,
    private val uuidConverter: Converter<String, UUID>
) : TasksRepository, Loggable {
    override val log = logger()
    override suspend fun add(
        name: String,
        description: String,
        dueDate: Instant,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        houseId: String,
        memberId: String,
        rotateMember: Boolean,
        createdDate: Instant
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
                task[Tasks.dueDate] = instantConverter.toStorage(dueDate)
                task[Tasks.repeatValue] = repeatValue
                task[Tasks.repeatUnit] = repeatUnit.key
                task[Tasks.houseId] = uuidConverter.toStorage(houseId)
                task[Tasks.memberId] = uuidConverter.toStorage(memberId)
                task[Tasks.rotateMember] = rotateMember
                task[Tasks.createdDate] = instantConverter.toStorage(createdDate)
            }
        }
        statement?.resultedValues?.get(0)?.let {
            return rowToTask(it)
        } ?: run {
            log.warning("Cannot convert row to Task")
            return null
        }
    }

    override suspend fun delete(): Int {
        return query {
            Tasks.deleteAll()
        }
    }

    override suspend fun delete(id: String): Int {
        return query {
            val uuid = uuidConverter.toStorage(id)
            Tasks.deleteWhere { Tasks.id.eq(uuid) }
        }
    }

    override suspend fun edit(
        id: String,
        name: String,
        description: String,
        dueDate: Instant,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        rotateMember: Boolean
    ): Int {
        return query {
            val uuid = uuidConverter.toStorage(id)
            Tasks.update({ Tasks.id.eq(uuid) }) { task ->
                task[Tasks.name] = name
                task[Tasks.description] = description
                task[Tasks.dueDate] = instantConverter.toStorage(dueDate)
                task[Tasks.repeatValue] = repeatValue
                task[Tasks.repeatUnit] = repeatUnit.key
                task[Tasks.rotateMember] = rotateMember
            }
        }
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

    private fun rowToTask(row: ResultRow): Task {
        val id = row[Tasks.id]
        val name = row[Tasks.name]
        val houseId = row[Tasks.houseId]
        val memberId = row[Tasks.memberId]
        val description = row[Tasks.description]
        val dueDate = instantConverter.toMain(row[Tasks.dueDate])
        val repeatValue = row[Tasks.repeatValue]
        val repeatUnit = RepeatUnit.fromKey(row[Tasks.repeatUnit])
        val createdDate = instantConverter.toMain(row[Tasks.createdDate])
        val rotateMember = row[Tasks.rotateMember]
        return Task(
            id.toString(),
            name,
            description,
            dueDate,
            repeatValue,
            repeatUnit,
            houseId.toString(),
            memberId.toString(),
            rotateMember,
            createdDate
        )
    }
}