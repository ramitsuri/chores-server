package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.MemberAssignment

interface MemberAssignmentsRepository {
    suspend fun add(memberId: String, houseId: String): MemberAssignment?

    suspend fun delete(): Int

    suspend fun delete(id: String): Int

    suspend fun get(): List<MemberAssignment>

    suspend fun get(id: String): MemberAssignment?

    suspend fun getForHouse(houseId: String): List<MemberAssignment>
}