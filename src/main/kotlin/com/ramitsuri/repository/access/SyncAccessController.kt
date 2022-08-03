package com.ramitsuri.repository.access

import com.ramitsuri.models.Access
import com.ramitsuri.models.AccessResult
import com.ramitsuri.models.SyncResult
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.SyncRepository

class SyncAccessController(
    private val syncRepository: SyncRepository,
    private val membersRepository: MembersRepository
) {
    suspend fun get(requesterMemberId: String): AccessResult<SyncResult> {
        return when (getAccess(requesterMemberId)) {
            Access.NONE -> {
                AccessResult.Failure
            }
            Access.READ_HOUSE_WRITE_OWN, Access.READ_HOUSE_WRITE_HOUSE -> {
                AccessResult.Success(syncRepository.getForMember(requesterMemberId))
            }
            Access.READ_ALL_WRITE_ALL -> {
                AccessResult.Success(syncRepository.get())
            }
        }
    }

    private suspend fun getAccess(memberId: String): Access = membersRepository.getAccess(memberId)
}