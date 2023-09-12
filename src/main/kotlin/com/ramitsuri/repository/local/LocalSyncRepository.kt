package com.ramitsuri.repository.local

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.House
import com.ramitsuri.models.MemberAssignment
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
        val memberAssignmentsForMember = memberAssignmentsRepository.getForMember(memberId)
        memberAssignmentsForMember.forEach { memberAssignment ->
            housesRepository.get(memberAssignment.houseId)?.let { house ->
                if (house.status == ActiveStatus.ACTIVE || house.status == ActiveStatus.PAUSED) {
                    associatedLists.add(house)
                }
            }
        }

        // Need to get all members of all houses the requesting member belongs in so that assigning tasks to other
        // members is possible from app clients
        val memberAssignmentsForAssociatedLists = mutableSetOf<MemberAssignment>()
        associatedLists.forEach { house ->
            val memberAssignments = memberAssignmentsRepository.getForHouse(house.id)
            memberAssignmentsForAssociatedLists.addAll(memberAssignments)
        }
        return SyncResult(
            associatedLists = associatedLists.toList(),
            memberListAssociations = memberAssignmentsForAssociatedLists.toList()
        )
    }

    override suspend fun get(): SyncResult {
        val associatedLists = mutableSetOf<House>()
        val memberAssignments = memberAssignmentsRepository.get()
        memberAssignments.forEach { memberAssignment ->
            housesRepository.get(memberAssignment.houseId)?.let { house ->
                if (house.status == ActiveStatus.ACTIVE || house.status == ActiveStatus.PAUSED) {
                    associatedLists.add(house)
                }
            }
        }
        return SyncResult(associatedLists = associatedLists.toList(), memberListAssociations = memberAssignments)
    }
}