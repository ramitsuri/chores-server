package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.MemberAssignments
import com.ramitsuri.models.Member
import com.ramitsuri.models.MemberAssignment
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import com.ramitsuri.repository.interfaces.MembersRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.util.*

class LocalMemberAssignmentsRepository(
    private val membersRepository: MembersRepository,
    private val housesRepository: HousesRepository,
    private val uuidConverter: Converter<String, UUID>
): MemberAssignmentsRepository {

    override suspend fun add(memberId: String, houseId: String): MemberAssignment? {
        val member = membersRepository.get(memberId) ?: return null
        housesRepository.get(houseId) ?: return null
        var statement: InsertStatement<Number>? = null
        DatabaseFactory.query {
            statement = MemberAssignments.insert {memberAssignment ->
                memberAssignment[MemberAssignments.memberId] = uuidConverter.toStorage(memberId)
                memberAssignment[MemberAssignments.houseId] = uuidConverter.toStorage(houseId)
            }
        }
        statement?.resultedValues?.get(0)?.let {
            return rowToMemberAssignment(it, member)
        }
        return null
    }

    override suspend fun delete(): Int {
        return DatabaseFactory.query {
            MemberAssignments.deleteAll()
        }
    }

    override suspend fun delete(id: String): Int {
        return DatabaseFactory.query {
            val uuid = uuidConverter.toStorage(id)
            MemberAssignments.deleteWhere {MemberAssignments.id.eq(uuid)}
        }
    }

    override suspend fun get(): List<MemberAssignment> {
        val members = membersRepository.get()
        return DatabaseFactory.query {
            MemberAssignments.selectAll().filterNotNull().mapNotNull {row ->
                val member = members.firstOrNull {it.id == rowToMemberId(row)}
                if (member != null) {
                    rowToMemberAssignment(row, member)
                } else {
                    null
                }
            }
        }
    }

    override suspend fun get(id: String): MemberAssignment? {
        val resultRow = DatabaseFactory.query {
            val uuid = uuidConverter.toStorage(id)
            MemberAssignments.select {MemberAssignments.id.eq(uuid)}.singleOrNull()
        } ?: return null
        val member = membersRepository.get(rowToMemberId(resultRow)) ?: return null
        return rowToMemberAssignment(resultRow, member)
    }

    override suspend fun getForHouse(houseId: String): List<MemberAssignment> {
        val members = membersRepository.get()
        return DatabaseFactory.query {
            val uuid = uuidConverter.toStorage(houseId)
            MemberAssignments.select {MemberAssignments.houseId.eq(uuid)}.filterNotNull().mapNotNull {row ->
                val member = members.firstOrNull {it.id == rowToMemberId(row)}
                if (member != null) {
                    rowToMemberAssignment(row, member)
                } else {
                    null
                }
            }
        }
    }

    private fun rowToMemberAssignment(row: ResultRow, member: Member): MemberAssignment {
        val id = row[MemberAssignments.id].toString()
        val houseId = uuidConverter.toMain(row[MemberAssignments.houseId])
        return MemberAssignment(id, member, houseId)
    }

    private fun rowToMemberId(row: ResultRow): String {
        return uuidConverter.toMain(row[MemberAssignments.memberId])
    }
}