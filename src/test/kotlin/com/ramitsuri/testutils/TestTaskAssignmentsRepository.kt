package com.ramitsuri.testutils

import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import java.time.Instant

class TestTaskAssignmentsRepository(
    private val tasksRepository: TasksRepository,
    private val membersRepository: MembersRepository
): BaseTestRepository<TaskAssignment>(), TaskAssignmentsRepository {
    override suspend fun add(
        progressStatus: ProgressStatus,
        statusDate: Instant,
        taskId: String,
        memberId: String,
        dueDate: Instant,
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

    override suspend fun delete(): Int {
        val size = storage.size
        storage.clear()
        return size
    }

    override suspend fun delete(id: String): Int {
        val toDelete = storage[id]
        return toDelete?.let {
            storage.remove(id)
            1
        } ?: run {
            0
        }
    }

    override suspend fun edit(id: String, progressStatus: ProgressStatus, statusDate: Instant): TaskAssignment? {
        val toEdit = storage[id]
        return toEdit?.let {
            val new = it.copy(progressStatus = progressStatus, progressStatusDate = statusDate)
            storage[id] = new
            storage[id]
        } ?: run {
            null
        }
    }

    override suspend fun get(): List<TaskAssignment> {
        return storage.values.toList()
    }

    override suspend fun get(filter: TaskAssignmentFilter): List<TaskAssignment> {
        TODO("Not yet implemented")
    }

    override suspend fun get(id: String): TaskAssignment? {
        return storage[id]
    }
}