package com.ramitsuri.repository.remote

import com.google.cloud.firestore.Firestore
import com.ramitsuri.data.Converter
import com.ramitsuri.extensions.wait
import com.ramitsuri.models.Member
import com.ramitsuri.models.MemberAssignment
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import com.ramitsuri.repository.interfaces.MembersRepository
import java.util.*

class RemoteMemberAssignmentsRepository(
    private val collection: String,
    private val db: Firestore,
    private val membersRepository: MembersRepository,
    private val housesRepository: HousesRepository,
    private val uuidConverter: Converter<String, UUID>
): MemberAssignmentsRepository {
    private val idColumn = "id"
    private val memberIdColumn = "memberId"
    private val houseIdColumn = "houseId"

    override suspend fun add(memberId: String, houseId: String): MemberAssignment? {
        val member = membersRepository.get(memberId) ?: return null
        housesRepository.get(houseId) ?: return null
        val id = uuidConverter.toMain(UUID.randomUUID())
        val map = mapOf<String, Any>(
            idColumn to id,
            memberIdColumn to memberId,
            houseIdColumn to houseId,
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
            MemberAssignment(id, member, houseId)
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

    override suspend fun get(): List<MemberAssignment> {
        val members = membersRepository.get()
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
            val list = mutableListOf<MemberAssignment>()
            for (document in result.documents) {
                val member = members.firstOrNull {it.id == toMemberId(document.data)}
                val memberAssignment = toMemberAssignment(document.data, member)
                if (memberAssignment != null) {
                    list.add(memberAssignment)
                }
            }
            list
        }
    }

    override suspend fun get(id: String): MemberAssignment? {
        val result = try {
            db.collection(collection)
                .document(id)
                .get()
                .wait()
        } catch (e: Exception) {
            null
        }
        result?.data?.let {data ->
            val memberId = toMemberId(data) ?: return null
            val member = membersRepository.get(memberId)
            return toMemberAssignment(data, member)
        }
        return null
    }

    override suspend fun getForHouse(houseId: String): List<MemberAssignment> {
        val members = membersRepository.get()
        val result = try {
            db.collection(collection)
                .whereEqualTo(houseIdColumn, houseId)
                .get()
                .wait()
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            listOf()
        } else {
            val list = mutableListOf<MemberAssignment>()
            for (document in result.documents) {
                val member = members.firstOrNull {it.id == toMemberId(document.data)}
                val memberAssignment = toMemberAssignment(document.data, member)
                if (memberAssignment != null) {
                    list.add(memberAssignment)
                }
            }
            list
        }
    }

    private fun toMemberId(data: Map<String, Any>): String? {
        return data[memberIdColumn] as? String
    }

    private fun toMemberAssignment(data: Map<String, Any>, member: Member?): MemberAssignment? {
        val id = data[idColumn] as? String
        val houseId = data[houseIdColumn] as? String
        if (id == null || member == null || houseId == null) {
            return null
        }
        return MemberAssignment(
            id,
            member,
            houseId,
        )
    }
}