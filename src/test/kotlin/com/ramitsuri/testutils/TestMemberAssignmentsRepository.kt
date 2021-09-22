package com.ramitsuri.testutils

import com.ramitsuri.models.MemberAssignment
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import com.ramitsuri.repository.interfaces.MembersRepository

class TestMemberAssignmentsRepository(
    private val membersRepository: MembersRepository,
    private val housesRepository: HousesRepository
): BaseTestRepository<MemberAssignment>(), MemberAssignmentsRepository {

    override suspend fun add(memberId: String, houseId: String): MemberAssignment? {
        val member = membersRepository.get(memberId) ?: return null
        housesRepository.get(houseId) ?: return null
        val id = getNewId()
        val new = MemberAssignment(id, member, houseId)
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

    override suspend fun get(): List<MemberAssignment> {
        return storage.values.toList()
    }

    override suspend fun get(id: String): MemberAssignment? {
        return storage[id]
    }

    override suspend fun getForHouse(houseId: String): List<MemberAssignment> {
        return storage.filter {it.value.houseId == houseId}.values.toList()
    }
}