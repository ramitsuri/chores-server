package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory.query
import com.ramitsuri.data.Members
import com.ramitsuri.models.Access
import com.ramitsuri.models.Member
import com.ramitsuri.repository.interfaces.MembersRepository
import com.toxicbakery.bcrypt.Bcrypt
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class LocalMembersRepository(
    private val instantConverter: Converter<Instant, String>,
    private val uuidConverter: Converter<String, UUID>
) : MembersRepository {
    override suspend fun add(name: String, createdDate: Instant): Member? {
        var statement: InsertStatement<Number>? = null
        query {
            statement = Members.insert { member ->
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
            Members.deleteWhere { Members.id.eq(uuid) }
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
            Members.update({ Members.id.eq(uuid) }) {
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
            Members.select { Members.id.eq(uuid) }.map {
                rowToMember(it)
            }.singleOrNull()
        }
    }

    override suspend fun getAuthenticated(id: String, key: String): Member? {
        return query {
            if (id.isEmpty() || key.isEmpty()) {
                return@query null
            }
            val uuid = uuidConverter.toStorage(id)
            val member = Members.select { Members.id.eq(uuid) }.map {
                rowToMemberWithKey(it)
            }.singleOrNull() ?: return@query null
            if (Bcrypt.verify(key, member.key.toByteArray())) {
                member
            } else {
                null
            }
        }
    }

    override suspend fun getAccess(id: String): Access {
        return query {
            val uuid = uuidConverter.toStorage(id)
            Members.select { Members.id.eq(uuid) }.map { row ->
                Access.fromKey(row[Members.access])
            }.singleOrNull() ?: Access.NONE
        }
    }

    private fun rowToMember(row: ResultRow): Member {
        val id = row[Members.id]
        val name = row[Members.name]
        val createdDate = instantConverter.toMain(row[Members.createdDate])
        return Member(id.toString(), name, createdDate)
    }

    private fun rowToMemberWithKey(row: ResultRow): Member {
        val id = row[Members.id]
        val name = row[Members.name]
        val key = row[Members.key]
        val createdDate = instantConverter.toMain(row[Members.createdDate])
        return Member(id.toString(), name, createdDate, key)
    }
}