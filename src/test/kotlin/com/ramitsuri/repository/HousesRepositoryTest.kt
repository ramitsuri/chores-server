package com.ramitsuri.repository

import com.ramitsuri.data.InstantConverter
import com.ramitsuri.data.UuidConverter
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.House
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.*

class HousesRepositoryTest: BaseRepositoryTest() {

    private val repository = HousesRepositoryImpl(UuidConverter(), InstantConverter())

    @Test
    fun testAdd() {
        runBlocking {
            add("House1")
            val houses = repository.get()
            assertNotNull(houses)
            assertEquals(1, houses.size)
            assertEquals("House1", houses[0].name)
        }
    }

    @Test
    fun testDelete_ifHouseExists_shouldDelete() {
        runBlocking {
            add("House1")
            var houses = repository.get()
            assertFalse(houses.isEmpty())
            repository.delete(houses[0].id)
            houses = repository.get()
            assertTrue(houses.isEmpty())
        }
    }

    @Test
    fun testDeleteAll_ofHousesExist_shouldDelete() {
        runBlocking {
            add("House1")
            add("House2")
            var houses = repository.get()
            assertFalse(houses.isEmpty())
            repository.delete()
            houses = repository.get()
            assertTrue(houses.isEmpty())
        }
    }

    @Test
    fun testDeleteAll_ifHousesDoNotExist_shouldDoNothing() {
        runBlocking {
            repository.delete()
        }
    }

    @Test
    fun testEdit() {
        runBlocking {
            add("House1")
            var house = repository.get().firstOrNull()
            repository.edit(house!!.id, "house2")
            house = repository.get(house.id)
            assertEquals("house2", house!!.name)
        }
    }

    @Test
    fun testGetAll() {
        runBlocking {
            val houses = repository.get()
            assertNotNull(houses)
            assertTrue(houses.isEmpty())
        }
    }

    @Test
    fun testEdit_ifHouseDoesNotExist_shouldDoNothing() {
        runBlocking {
            repository.edit(UUID.randomUUID().toString(), "house2")
        }
    }

    private suspend fun add(name: String): House? {
        return repository.add(name, UUID.randomUUID().toString(), Instant.now(), ActiveStatus.ACTIVE)
    }
}