package com.ramitsuri.testutils

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.House
import com.ramitsuri.repository.interfaces.HousesRepository
import java.time.Instant

class TestHousesRepository: BaseTestRepository<House>(), HousesRepository {

    override suspend fun add(
        name: String,
        createdByMemberId: String,
        createdDate: Instant,
        status: ActiveStatus
    ): House? {
        val id = getNewId()
        val new = House(id, name, createdByMemberId, createdDate, status)
        storage[id] = new
        return new
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

    override suspend fun delete(): Int {
        val size = storage.size
        storage.clear()
        return size
    }

    override suspend fun edit(id: String, name: String): Int {
        val toEdit = storage[id]
        return toEdit?.let {
            val new = it.copy(name = name)
            storage[id] = new
            1
        } ?: run {
            0
        }
    }

    override suspend fun get(): List<House> {
        return storage.values.toList()
    }

    override suspend fun get(id: String): House? {
        return storage[id]
    }
}