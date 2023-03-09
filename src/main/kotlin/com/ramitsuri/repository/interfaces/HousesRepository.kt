package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.House
import java.time.Instant

interface HousesRepository {
    suspend fun add(name: String, createdByMemberId: String, createdDate: Instant, status: ActiveStatus): House?

    suspend fun delete(id: String): Int

    suspend fun edit(id: String, name: String): Int

    suspend fun get(): List<House>

    suspend fun get(id: String): House?
}