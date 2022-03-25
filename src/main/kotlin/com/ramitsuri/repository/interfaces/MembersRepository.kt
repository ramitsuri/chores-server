package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.Member
import com.ramitsuri.models.Access
import java.time.Instant

interface MembersRepository {
    suspend fun add(name: String, createdDate: Instant): Member?

    suspend fun delete(id: String): Int

    suspend fun delete(): Int

    suspend fun edit(id: String, name: String): Int

    suspend fun get(): List<Member>

    suspend fun get(id: String): Member?

    suspend fun getAuthenticated(id: String, key: String): Member?

    suspend fun getAccess(id: String): Access
}