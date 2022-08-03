package com.ramitsuri.repository.local

import com.ramitsuri.models.House
import com.ramitsuri.models.SyncResult
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import com.ramitsuri.repository.interfaces.SyncRepository

class LocalSyncRepository(
    private val memberAssignmentsRepository: MemberAssignmentsRepository,
    private val housesRepository: HousesRepository
) : SyncRepository {
    override suspend fun getForMember(memberId: String): SyncResult {
        val associatedLists = mutableSetOf<House>()
        val memberAssignments = memberAssignmentsRepository.getForMember(memberId)
        memberAssignments.forEach { memberAssignment ->
            housesRepository.get(memberAssignment.houseId)?.let { associatedLists.add(it) }
        }
        return SyncResult(associatedLists = associatedLists.toList())
    }

    override suspend fun get(): SyncResult {
        val associatedLists = mutableSetOf<House>()
        val memberAssignments = memberAssignmentsRepository.get()
        memberAssignments.forEach { memberAssignment ->
            housesRepository.get(memberAssignment.houseId)?.let { associatedLists.add(it) }
        }
        return SyncResult(associatedLists = associatedLists.toList())
    }
}