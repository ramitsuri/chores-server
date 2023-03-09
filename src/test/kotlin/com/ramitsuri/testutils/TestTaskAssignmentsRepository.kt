package com.ramitsuri.testutils

import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.models.TaskAssignmentDto
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import java.time.Instant
import java.time.LocalDateTime

class TestTaskAssignmentsRepository(
    private val tasksRepository: TasksRepository,
    private val membersRepository: MembersRepository
) : BaseTestRepository<TaskAssignment>(), TaskAssignmentsRepository {
    override suspend fun add(
        progressStatus: ProgressStatus,
        statusDate: Instant,
        taskId: String,
        memberId: String,
        dueDate: LocalDateTime,
        createdDate: Instant,
        createType: CreateType
    ): TaskAssignment? {
        val task = tasksRepository.get(taskId) ?: return null
        val member = membersRepository.get(memberId) ?: return null
        val id = getNewId()
        val new = TaskAssignment(id, progressStatus, statusDate, task, member, dueDate, createdDate, createType)
        storage[id] = new
        return new
    }

    fun delete(): Int {
        val size = storage.size
        storage.clear()
        return size
    }

    override suspend fun get(): List<TaskAssignment> {
        return storage.values.toList()
    }

    override suspend fun get(filter: TaskAssignmentFilter): List<TaskAssignment> {
        TODO("Not yet implemented")
    }

    override suspend fun getForHouse(filter: TaskAssignmentFilter, houseIds: List<String>): List<TaskAssignment> {
        TODO("Not yet implemented")
    }

    override suspend fun edit(taskAssignments: List<TaskAssignmentDto>): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun editOwn(taskAssignments: List<TaskAssignmentDto>, requesterMemberId: String): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun editForHouse(taskAssignments: List<TaskAssignmentDto>, houseIds: List<String>): List<String> {
        TODO("Not yet implemented")
    }
}