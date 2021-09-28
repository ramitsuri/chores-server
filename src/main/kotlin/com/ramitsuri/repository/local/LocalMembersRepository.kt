package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory.query
import com.ramitsuri.data.Members
import com.ramitsuri.models.Member
import com.ramitsuri.repository.interfaces.MembersRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.time.Instant
import java.util.*

class LocalMembersRepository(
    private val instantConverter: Converter<Instant, String>,
    private val uuidConverter: Converter<String, UUID>
): MembersRepository {
    override suspend fun add(name: String, createdDate: Instant): Member? {
        var statement: InsertStatement<Number>? = null
        query {
            statement = Members.insert {member ->
                member[Members.name] = name
                member[Members.createdDate] = instantConverter.toStorage(createdDate)
            }
        }
        statement?.resultedValues?.get(0)?.let {
            return rowToMember(it)
        }
        return null
    }

    override suspend fun delete(id: String): Int {
        return query {
            val uuid = uuidConverter.toStorage(id)
            Members.deleteWhere {Members.id.eq(uuid)}
        }
    }

    override suspend fun delete(): Int {
        return query {
            Members.deleteAll()
        }
    }

    override suspend fun edit(id: String, name: String): Int {
        return query {
            val uuid = uuidConverter.toStorage(id)
            Members.update({Members.id.eq(uuid)}) {
                it[Members.name] = name
            }
        }
    }

    override suspend fun get(): List<Member> {
        return query {
            Members.selectAll().filterNotNull().map {
                rowToMember(it)
            }
        }
    }

    override suspend fun get(id: String): Member? {
        return query {
            val uuid = uuidConverter.toStorage(id)
            Members.select {Members.id.eq(uuid)}.map {
                rowToMember(it)
            }.singleOrNull()
        }
    }

    private fun rowToMember(row: ResultRow): Member {
        val id = row[Members.id]
        val name = row[Members.name]
        val createdDate = instantConverter.toMain(row[Members.createdDate])
        return Member(id.toString(), name, createdDate)
    }
}