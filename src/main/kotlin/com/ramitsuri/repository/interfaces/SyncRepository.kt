package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.SyncResult

interface SyncRepository {
    suspend fun getForMember(memberId: String): SyncResult
    suspend fun get(): SyncResult
}