package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.RunTimeLogs
import com.ramitsuri.repository.interfaces.RunTimeLogsRepository
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.time.Instant

class LocalRunTimeLogsRepository(
    private val instantConverter: Converter<Instant, String>
) : RunTimeLogsRepository {

    override suspend fun add(runTime: Instant): Instant? {
        var statement: InsertStatement<Number>? = null
        DatabaseFactory.query {
            RunTimeLogs.deleteAll()
            statement = RunTimeLogs.insert { runTimeLog ->
                runTimeLog[RunTimeLogs.runTime] = instantConverter.toStorage(runTime)
            }
        }
        statement?.resultedValues?.get(0)?.let { row ->
            return instantConverter.toMain(row[RunTimeLogs.runTime])
        }
        return null
    }

    override suspend fun get(): Instant? {
        val row = DatabaseFactory.query {
            RunTimeLogs.selectAll().firstOrNull()
        } ?: return null
        return instantConverter.toMain(row[RunTimeLogs.runTime])
    }
}