package com.ramitsuri.repository.access

import com.ramitsuri.models.Access
import com.ramitsuri.models.AccessResult
import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.models.TaskAssignmentDto
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository

class TaskAssignmentAccessController(
    private val membersRepository: MembersRepository,
    private val memberAssignmentsRepository: MemberAssignmentsRepository,
    private val taskAssignmentsRepository: TaskAssignmentsRepository
) {

    suspend fun get(requesterMemberId: String, filter: TaskAssignmentFilter): AccessResult<List<TaskAssignment>> {
        return when (getAccess(requesterMemberId)) {
            Access.NONE -> {
                AccessResult.Failure
            }
            Access.READ_HOUSE_WRITE_OWN, Access.READ_HOUSE_WRITE_HOUSE -> {
                val houseIds = memberAssignmentsRepository.getForMember(requesterMemberId).map { it.houseId }
                AccessResult.Success(taskAssignmentsRepository.getForHouse(filter, houseIds))
            }
            Access.READ_ALL_WRITE_ALL -> {
                AccessResult.Success(taskAssignmentsRepository.get(filter))
            }
        }
    }

    suspend fun edit(requesterMemberId: String, taskAssignments: List<TaskAssignmentDto>): AccessResult<List<String>> {
        return when (getAccess(requesterMemberId)) {
            Access.NONE -> {
                AccessResult.Failure
            }
            Access.READ_HOUSE_WRITE_OWN -> {
                AccessResult.Success(taskAssignmentsRepository.editOwn(taskAssignments, requesterMemberId))
            }
            Access.READ_HOUSE_WRITE_HOUSE -> {
                val houseIds = memberAssignmentsRepository.getForMember(requesterMemberId).map { it.houseId }
                AccessResult.Success(taskAssignmentsRepository.editForHouse(taskAssignments, houseIds))
            }
            Access.READ_ALL_WRITE_ALL -> {
                AccessResult.Success(taskAssignmentsRepository.edit(taskAssignments))
            }
        }
    }

    private suspend fun getAccess(memberId: String): Access = membersRepository.getAccess(memberId)
}