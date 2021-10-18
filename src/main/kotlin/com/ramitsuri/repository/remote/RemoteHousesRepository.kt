package com.ramitsuri.repository.remote

import com.google.cloud.firestore.Firestore
import com.ramitsuri.data.Converter
import com.ramitsuri.extensions.wait
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.House
import com.ramitsuri.repository.interfaces.HousesRepository
import java.time.Instant
import java.util.*

class RemoteHousesRepository(
    private val collection: String,
    private val db: Firestore,
    private val uuidConverter: Converter<String, UUID>,
    private val instantConverter: Converter<Instant, String>
): HousesRepository {
    private val idColumn = "id"
    private val nameColumn = "name"
    private val memberIdColumn = "memberId"
    private val createdDateColumn = "createdDate"
    private val activeStatusColumn = "activeStatus"

    override suspend fun add(
        name: String,
        createdByMemberId: String,
        createdDate: Instant,
        status: ActiveStatus
    ): House? {
        val id = uuidConverter.toMain(UUID.randomUUID())
        val map = mapOf<String, Any>(
            idColumn to id,
            nameColumn to name,
            memberIdColumn to createdByMemberId,
            createdDateColumn to instantConverter.toStorage(createdDate),
            activeStatusColumn to status.key
        )
        val result = try {
            db.collection(collection)
                .document(id)
                .set(map).wait()
        } catch (e: Exception) {
            null
        }

        return if (result == null) {
            null
        } else {
            House(id, name, createdByMemberId, createdDate, status)
        }
    }

    override suspend fun delete(id: String): Int {
        val result = try {
            db.collection(collection)
                .document(id)
                .update(activeStatusColumn, ActiveStatus.INACTIVE.key)
                .wait()
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            0
        } else {
            1
        }
    }

    override suspend fun delete(): Int {
        var count = 0
        val result = try {
            var repeat = true
            while (repeat) {
                val documents = db.collection(collection)
                    .whereEqualTo(activeStatusColumn, ActiveStatus.ACTIVE.key)
                    .limit(25)
                    .get()
                    .wait()
                repeat = !documents.isEmpty
                count += documents.size()
                for (document in documents) {
                    document.reference.update(activeStatusColumn, ActiveStatus.INACTIVE.key)
                }
            }
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            0
        } else {
            count
        }
    }

    override suspend fun edit(id: String, name: String): Int {
        val result = try {
            db.collection(collection).document(id).update(nameColumn, name)
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            0
        } else {
            1
        }
    }

    override suspend fun get(): List<House> {
        val result = try {
            db.collection(collection)
                .whereEqualTo(activeStatusColumn, ActiveStatus.ACTIVE.key)
                .get()
                .wait()
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            listOf()
        } else {
            val list = mutableListOf<House>()
            for (document in result.documents) {
                val house = toHouse(document.data)
                if (house != null) {
                    list.add(house)
                }
            }
            list
        }
    }

    override suspend fun get(id: String): House? {
        val result = try {
            db.collection(collection)
                .document(id)
                .get()
                .wait()
        } catch (e: Exception) {
            null
        }
        result?.data?.let {
            return toHouse(it)
        }
        return null
    }

    private fun toHouse(data: Map<String, Any>): House? {
        val id = data[idColumn] as? String
        val name = data[nameColumn] as? String
        val createdByMemberId = data[memberIdColumn] as? String
        val createdDate = data[createdDateColumn] as? String
        val status = data[activeStatusColumn] as? Long
        if (id == null || name == null || createdByMemberId == null || createdDate == null || status == null) {
            return null
        }
        return House(
            id,
            name,
            createdByMemberId,
            instantConverter.toMain(createdDate),
            ActiveStatus.fromKey(status.toInt())
        )
    }
}