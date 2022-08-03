package com.ramitsuri.repository.access

import com.ramitsuri.models.Access
import com.ramitsuri.repository.interfaces.MembersRepository

abstract class AccessController(private val membersRepository: MembersRepository) {
    protected suspend fun getAccess(memberId: String): Access = membersRepository.getAccess(memberId)
}