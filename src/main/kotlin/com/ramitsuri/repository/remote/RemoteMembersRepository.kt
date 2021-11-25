package com.ramitsuri.repository.remote

import com.google.cloud.firestore.Firestore
import com.ramitsuri.data.Converter
import com.ramitsuri.data.Houses.createdByMemberId
import com.ramitsuri.extensions.wait
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.House
import com.ramitsuri.models.Member
import com.ramitsuri.repository.interfaces.MembersRepository
import java.time.Instant
import java.util.*

class RemoteMembersRepository(
    private val collection: String,
    private val db: Firestore,
    private val instantConverter: Converter<Instant, String>,
    private val uuidConverter: Converter<String, UUID>
): MembersRepository {
    private val idColumn = "id"
    private val nameColumn = "name"
    private val createdDateColumn = "createdDate"

    override suspend fun add(name: String, createdDate: Instant): Member? {
        val id = uuidConverter.toMain(UUID.randomUUID())
        val map = mapOf<String, Any>(
            idColumn to id,
            nameColumn to name,
            createdDateColumn to instantConverter.toStorage(createdDate),
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
            Member(id, name, createdDate)
        }
    }

    override suspend fun delete(id: String): Int {
        val result = try {
            db.collection(collection)
                .document(id)
                .delete()
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
                    .limit(25)
                    .get()
                    .wait()
                repeat = !documents.isEmpty
                count += documents.size()
                for (document in documents) {
                    document.reference.delete()
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

    override suspend fun get(): List<Member> {
        val result = try {
            db.collection(collection)
                .get()
                .wait()
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            listOf()
        } else {
            val list = mutableListOf<Member>()
            for (document in result.documents) {
                val member = toMember(document.data)
                if (member != null) {
                    list.add(member)
                }
            }
            list
        }
    }

    override suspend fun get(id: String): Member? {
        val result = try {
            db.collection(collection)
                .document(id)
                .get()
                .wait()
        } catch (e: Exception) {
            null
        }
        result?.data?.let {
            return toMember(it)
        }
        return null
    }

    private fun toMember(data: Map<String, Any>): Member? {
        val id = data[idColumn] as? String
        val name = data[nameColumn] as? String
        val createdDate = data[createdDateColumn] as? String
        if (id == null || name == null || createdDate == null) {
            return null
        }
        return Member(
            id,
            name,
            instantConverter.toMain(createdDate),
        )
    }
}