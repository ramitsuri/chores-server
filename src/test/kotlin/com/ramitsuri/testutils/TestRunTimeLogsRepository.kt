package com.ramitsuri.testutils

import com.ramitsuri.repository.interfaces.RunTimeLogsRepository
import java.time.Instant

class TestRunTimeLogsRepository : BaseTestRepository<Instant>(), RunTimeLogsRepository {
    override suspend fun add(runTime: Instant): Instant? {
        storage.clear()
        val id = getNewId()
        storage[id] = runTime
        return runTime
    }

    override suspend fun get(): Instant? {
        return storage.values.toList().firstOrNull()
    }
}