package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.PushMessageToken

interface PushMessageTokenRepository {
    suspend fun addOrReplace(memberId: String, deviceId: String, token: String): PushMessageToken?

    suspend fun getForMember(memberId: String): List<PushMessageToken>
}