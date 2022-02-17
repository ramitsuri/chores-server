package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import java.time.Instant

interface TasksRepository {
    suspend fun add(
        name: String,
        description: String,
        dueDate: Instant,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        houseId: String,
        memberId: String,
        rotateMember: Boolean,
        createdDate: Instant
    ): Task?

    suspend fun delete(): Int

    suspend fun delete(id: String): Int

    suspend fun edit(
        id: String,
        name: String,
        description: String,
        dueDate: Instant,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        rotateMember: Boolean
    ): Int

    suspend fun get(): List<Task>

    suspend fun get(id: String): Task?
}