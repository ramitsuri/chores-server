package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.PushMessageToken
import java.time.Instant

interface PushMessageTokenRepository {
    suspend fun addOrReplace(
        memberId: String,
        deviceId: String,
        token: String,
        addedDateTime: Instant
    ): PushMessageToken?

    suspend fun getOfMembers(memberIds: List<String>, now: Instant): List<PushMessageToken>
}