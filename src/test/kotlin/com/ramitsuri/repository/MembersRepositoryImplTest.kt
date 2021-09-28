package com.ramitsuri.repository

import com.ramitsuri.data.InstantConverter
import com.ramitsuri.data.UuidConverter
import com.ramitsuri.repository.local.LocalMembersRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class MembersRepositoryImplTest: BaseRepositoryTest() {
    private val repository = LocalMembersRepository(InstantConverter(), UuidConverter())

    @Test
    fun testAdd_shouldAdd() {
        runBlocking {
            repository.add("Member1", Instant.now())
            val members = repository.get()
            assertNotNull(members)
        }
    }

    @Test
    fun testDeleteSingle() {
        runBlocking {
            val member = repository.add("Member1", Instant.now())
            repository.add("Member2", Instant.now())
            assertNotNull(member)
            repository.delete(member!!.id)
            val members = repository.get()
            assertTrue(members.isNotEmpty())
            assertTrue(members.size == 1)
        }
    }

    @Test
    fun testDeleteAll() {
        runBlocking {
            repository.add("Member1", Instant.now())
            repository.add("Member2", Instant.now())
            var members = repository.get()
            assertTrue(members.isNotEmpty())
            repository.delete()
            members = repository.get()
            assertTrue(members.isEmpty())
        }
    }

    @Test
    fun testEdit() {
        runBlocking {
            var member = repository.add("Member1", Instant.now())
            repository.edit(member!!.id, "member2")
            member = repository.get(member.id)
            assertEquals("member2", member!!.name)
        }
    }
}