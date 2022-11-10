package com.ramitsuri.repository.interfaces

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import java.time.Instant
import java.time.LocalDateTime

interface TasksRepository {
    suspend fun add(
        name: String,
        description: String,
        dueDate: LocalDateTime,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        houseId: String,
        memberId: String,
        rotateMember: Boolean,
        createdDate: Instant,
        status: ActiveStatus
    ): Task?

    suspend fun delete(): Int

    suspend fun delete(id: String): Int

    suspend fun edit(
        id: String,
        name: String,
        description: String,
        dueDate: LocalDateTime,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        rotateMember: Boolean,
        status: ActiveStatus
    ): Int

    suspend fun get(): List<Task>

    suspend fun getForHouses(houseIds: List<String>): List<Task>

    suspend fun get(id: String): Task?
}