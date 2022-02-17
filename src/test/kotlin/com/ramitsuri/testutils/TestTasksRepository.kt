package com.ramitsuri.testutils

import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import java.time.Instant

class TestTasksRepository(
    private val housesRepository: HousesRepository
): BaseTestRepository<Task>(), TasksRepository {

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
        housesRepository.get(houseId) ?: return null
        val id = getNewId()
        val new =
            Task(id, name, description, dueDate, repeatValue, repeatUnit, houseId, memberId, rotateMember, createdDate)
        storage[id] = new
        return new
    }

    override suspend fun delete(): Int {
        val size = storage.size
        storage.clear()
        return size
    }

    override suspend fun delete(id: String): Int {
        val toDelete = storage[id]
        return toDelete?.let {
            storage.remove(id)
            1
        } ?: run {
            0
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
        val toEdit = storage[id]
        return toEdit?.let {

            val new =
                it.copy(
                    name = name,
                    description = description,
                    dueDateTime = dueDate,
                    repeatValue = repeatValue,
                    repeatUnit = repeatUnit,
                    rotateMember = rotateMember
                )
            storage[id] = new
            1
        } ?: run {
            0
        }
    }

    override suspend fun get(): List<Task> {
        return storage.values.toList()
    }

    override suspend fun get(id: String): Task? {
        return storage[id]
    }
}