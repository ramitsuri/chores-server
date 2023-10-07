package com.ramitsuri.testutils

import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.models.TaskAssignmentDto
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import com.ramitsuri.repository.interfaces.TaskAssignmentInsert
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import com.ramitsuri.repository.interfaces.TasksRepository

class TestTaskAssignmentsRepository(
    private val tasksRepository: TasksRepository,
    private val membersRepository: MembersRepository
) : BaseTestRepository<TaskAssignment>(), TaskAssignmentsRepository {

    fun delete(): Int {
        val size = storage.size
        storage.clear()
        return size
    }

    fun get(): List<TaskAssignment> {
        return storage.values.toList()
    }

    override suspend fun getMostRecentForTask(taskId: String): TaskAssignment? {
        return storage.values
            .filter { it.task.id == taskId }
            .maxByOrNull { it.dueDateTime }
    }

    override suspend fun get(filter: TaskAssignmentFilter): List<TaskAssignment> {
        TODO("Not yet implemented")
    }

    override suspend fun getForHouse(filter: TaskAssignmentFilter, houseIds: List<String>): List<TaskAssignment> {
        TODO("Not yet implemented")
    }

    override suspend fun edit(taskAssignments: List<TaskAssignmentDto>, requesterMemberId: String): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun editOwn(taskAssignments: List<TaskAssignmentDto>, requesterMemberId: String): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun add(taskAssignments: List<TaskAssignmentInsert>) {
        taskAssignments.forEach {
            val task = tasksRepository.get(it.taskId) ?: return@forEach
            val member = membersRepository.get(it.memberId) ?: return@forEach
            val id = getNewId()
            val new =
                TaskAssignment(
                    id,
                    it.progressStatus,
                    it.progressStatusDateTime,
                    task,
                    member,
                    it.dueDateTime,
                    it.createdDateTime,
                    it.createType,
                    null
                )
            storage[id] = new
        }
    }

    override suspend fun get(taskAssignmentIds: List<String>): List<TaskAssignment> {
        TODO("Not yet implemented")
    }

    override suspend fun editForHouse(
        taskAssignments: List<TaskAssignmentDto>,
        houseIds: List<String>,
        requesterMemberId: String
    ): List<String> {
        TODO("Not yet implemented")
    }
}