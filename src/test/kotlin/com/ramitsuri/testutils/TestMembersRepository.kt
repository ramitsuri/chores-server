package com.ramitsuri.testutils

import com.ramitsuri.models.Member
import com.ramitsuri.repository.interfaces.MembersRepository
import com.toxicbakery.bcrypt.Bcrypt
import java.time.Instant

class TestMembersRepository : BaseTestRepository<Member>(), MembersRepository {

    override suspend fun add(name: String, createdDate: Instant): Member? {
        val id = getNewId()
        val new = Member(id, name, createdDate)
        storage[id] = new
        return new
    }

    override suspend fun delete(id: String): Int {
        val toDelete = storage[id]
        return toDelete?.let {
            storage.remove(id)
            1
        } ?: run {
            0
        }
    }

    override suspend fun delete(): Int {
        val size = storage.size
        storage.clear()
        return size
    }

    override suspend fun edit(id: String, name: String): Int {
        val toEdit = storage[id]
        return toEdit?.let {
            val new = it.copy(name = name)
            storage[id] = new
            1
        } ?: run {
            0
        }
    }

    override suspend fun get(): List<Member> {
        return storage.values.toList()
    }

    override suspend fun get(id: String): Member? {
        return storage[id]?.copy(key = "")
    }

    override suspend fun getAuthenticated(id: String, key: String): Member? {
        val member = storage[id] ?: return null
        return if (Bcrypt.verify(key, member.key.toByteArray())) {
            member
        } else {
            null
        }
    }
}