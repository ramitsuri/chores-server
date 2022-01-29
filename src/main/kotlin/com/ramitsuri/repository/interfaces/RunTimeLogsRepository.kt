package com.ramitsuri.repository.interfaces

import java.time.Instant

interface RunTimeLogsRepository {
    suspend fun add(runTime: Instant): Instant?
    suspend fun get(): Instant?
}