package com.ramitsuri.testutils

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.acos

class TestTasksRepository(
    private val housesRepository: HousesRepository
) : BaseTestRepository<Task>(), TasksRepository {

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
        housesRepository.get(houseId) ?: return null
        val id = getNewId()
        val new =
            Task(
                id,
                name,
                description,
                dueDate,
                repeatValue,
                repeatUnit,
                repeatEndDate,
                houseId,
                memberId,
                rotateMember,
                createdDate,
                status
            )
        storage[id] = new
        return new
    }

    override suspend fun delete(): Int {
        val size = storage.size
        storage.clear()
        return size
    }

    override suspend fun delete(id: String): Boolean {
        val toDelete = storage[id]
        return toDelete?.let {
            storage.remove(id)
            true
        } ?: run {
            false
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
        val toEdit = storage[id]
        return toEdit?.let {

            val new =
                it.copy(
                    name = name,
                    description = description,
                    dueDateTime = dueDate,
                    repeatValue = repeatValue,
                    repeatUnit = repeatUnit,
                    repeatEndDateTime = repeatEndDate,
                    rotateMember = rotateMember,
                    status = status
                )
            storage[id] = new
            true
        } ?: run {
            false
        }
    }

    override suspend fun get(): List<Task> {
        return storage.values.toList()
    }

    override suspend fun get(id: String): Task? {
        return storage[id]
    }

    override suspend fun getForHouses(houseIds: List<String>): List<Task> {
        TODO("Not yet implemented")
    }
}