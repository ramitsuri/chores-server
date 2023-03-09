package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory.query
import com.ramitsuri.data.Houses
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.House
import com.ramitsuri.repository.interfaces.HousesRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.time.Instant
import java.util.*

class LocalHousesRepository(
    private val uuidConverter: Converter<String, UUID>,
    private val instantConverter: Converter<Instant, String>
): HousesRepository {
    override suspend fun add(
        name: String,
        createdByMemberId: String,
        createdDate: Instant,
        status: ActiveStatus
    ): House? {
        var statement: InsertStatement<Number>? = null
        query {
            statement = Houses.insert {house ->
                house[Houses.name] = name
                house[Houses.createdByMemberId] = uuidConverter.toStorage(createdByMemberId)
                house[Houses.createdDate] = instantConverter.toStorage(createdDate)
                house[Houses.activeStatus] = status.key
            }
        }
        statement?.resultedValues?.get(0)?.let {
            return rowToHouse(it)
        }
        return null
    }

    override suspend fun delete(id: String): Int {
        return query {
            val uuid = uuidConverter.toStorage(id)
            Houses.deleteWhere {Houses.id.eq(uuid)}
        }
    }

    override suspend fun edit(id: String, name: String): Int {
        return query {
            val uuid = uuidConverter.toStorage(id)
            Houses.update({Houses.id.eq(uuid)}) {
                it[Houses.name] = name
            }
        }
    }

    override suspend fun get(): List<House> {
        return query {
            Houses.selectAll().filterNotNull().map {
                rowToHouse(it)
            }
        }
    }

    override suspend fun get(id: String): House? {
        return query {
            val uuid = uuidConverter.toStorage(id)
            Houses.select {Houses.id.eq(uuid)}.map {
                rowToHouse(it)
            }.singleOrNull()
        }
    }

    private fun rowToHouse(row: ResultRow): House {
        val id = row[Houses.id]
        val name = row[Houses.name]
        val createdByMemberId = uuidConverter.toMain(row[Houses.createdByMemberId])
        val createdDate = instantConverter.toMain(row[Houses.createdDate])
        val status = ActiveStatus.fromKey(row[Houses.activeStatus])
        return House(id.toString(), name, createdByMemberId, createdDate, status)
    }
}