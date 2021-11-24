package com.ramitsuri.repository.remote

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.ramitsuri.data.Converter
import com.ramitsuri.extensions.wait
import com.ramitsuri.models.*
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import java.time.Instant
import java.util.*

class RemoteTaskAssignmentsRepository(
    private val collection: String,
    private val db: Firestore,
    private val tasksRepository: TasksRepository,
    private val membersRepository: MembersRepository,
    private val instantConverter: Converter<Instant, String>,
    private val uuidConverter: Converter<String, UUID>
) : TaskAssignmentsRepository {
    private val idColumn = "id"
    private val statusTypeColumn = "statusType"
    private val statusDateColumn = "statusDate"
    private val memberIdColumn = "memberId"
    private val taskIdColumn = "taskId"
    private val dueDateColumn = "dueDate"
    private val createdDateColumn = "createdDate"
    private val createTypeColumn = "createType"

    suspend fun rows(): Int {
        val result = try {
            db.collection(collection)
                .get()
                .wait()
        } catch (e: Exception) {
            null
        }
        return result?.documents?.size ?: 0
    }

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
        val id = uuidConverter.toMain(UUID.randomUUID())
        val map = mapOf<String, Any>(
            idColumn to id,
            statusTypeColumn to progressStatus.key,
            statusDateColumn to instantConverter.toStorage(statusDate),
            memberIdColumn to memberId,
            taskIdColumn to taskId,
            dueDateColumn to instantConverter.toStorage(dueDate),
            createdDateColumn to instantConverter.toStorage(createdDate),
            createTypeColumn to createType.key
        )
        val result = try {
            db.collection(collection)
                .document(id)
                .set(map).wait()
        } catch (e: Exception) {
            null
        }

        return if (result == null) {
            null
        } else {
            TaskAssignment(id, progressStatus, statusDate, task, member, dueDate, createdDate, createType)
        }
    }

    override suspend fun delete(): Int {
        var count = 0
        val result = try {
            var repeat = true
            while (repeat) {
                val documents = db.collection(collection)
                    .limit(25)
                    .get()
                    .wait()
                repeat = !documents.isEmpty
                count += documents.size()
                for (document in documents) {
                    document.reference.delete()
                }
            }
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            0
        } else {
            count
        }
    }

    override suspend fun delete(id: String): Int {
        val result = try {
            db.collection(collection)
                .document(id)
                .delete()
                .wait()
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            0
        } else {
            1
        }
    }

    override suspend fun edit(id: String, progressStatus: ProgressStatus, statusDate: Instant): TaskAssignment? {
        val result = try {
            val map = mapOf<String, Any>(
                statusTypeColumn to progressStatus.key,
                statusDateColumn to instantConverter.toStorage(statusDate)
            )
            db.collection(collection).document(id).update(map).wait()
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            null
        } else {
            get(id)
        }
    }

    override suspend fun get(): List<TaskAssignment> {
        val members = membersRepository.get()
        val tasks = tasksRepository.get()
        val result = try {
            db.collection(collection)
                .get()
                .wait()
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            listOf()
        } else {
            val list = mutableListOf<TaskAssignment>()
            for (document in result.documents) {
                val member = members.firstOrNull { it.id == toMemberId(document.data) }
                val task = tasks.firstOrNull { it.id == toTaskId(document.data) }
                val taskAssignment = toTaskAssignment(document.data, task, member)
                if (taskAssignment != null) {
                    list.add(taskAssignment)
                }
            }
            list
        }
    }

    override suspend fun get(filter: TaskAssignmentFilter): List<TaskAssignment> {
        val members = membersRepository.get()
        val tasks = tasksRepository.get()
        val assignments = mutableListOf<TaskAssignment>()
        try {
            var repeat = true
            while (repeat) {
                var query: Query = db.collection(collection)
                if (!filter.memberId.isNullOrEmpty()) {
                    query = query.whereEqualTo(memberIdColumn, filter.memberId)
                }
                if (!filter.notMemberId.isNullOrEmpty() && filter.notMemberId != filter.memberId) {
                    query = query.whereNotEqualTo(memberIdColumn, filter.notMemberId)
                }
                if (filter.progressStatus != ProgressStatus.UNKNOWN) {
                    query = query.whereEqualTo(statusTypeColumn, filter.progressStatus.key)
                }
                val result = query
                    .limit(25)
                    .get()
                    .wait()

                repeat = !result.isEmpty
                for (document in result.documents) {
                    val member = members.firstOrNull { it.id == toMemberId(document.data) }
                    val task = tasks.firstOrNull { it.id == toTaskId(document.data) }
                    val taskAssignment = toTaskAssignment(document.data, task, member)
                    if (taskAssignment != null) {
                        assignments.add(taskAssignment)
                    }
                }
            }
        } catch (e: Exception) {
            // Do nothing
        }
        return assignments
    }

    override suspend fun get(id: String): TaskAssignment? {
        val result = try {
            db.collection(collection)
                .document(id)
                .get()
                .wait()
        } catch (e: Exception) {
            null
        }
        result?.data?.let { data ->
            val memberId = toMemberId(data) ?: return null
            val member = membersRepository.get(memberId)
            val taskId = toTaskId(data) ?: return null
            val task = tasksRepository.get(taskId)
            return toTaskAssignment(data, task, member)
        }
        return null
    }

    private fun toMemberId(data: Map<String, Any>): String? {
        return data[memberIdColumn] as? String
    }

    private fun toTaskId(data: Map<String, Any>): String? {
        return data[taskIdColumn] as? String
    }

    private fun toTaskAssignment(data: Map<String, Any>, task: Task?, member: Member?): TaskAssignment? {
        val id = data[idColumn] as? String
        val statusDate = data[statusDateColumn] as? String
        val progressStatus = data[statusTypeColumn] as? Long
        val dueDate = data[dueDateColumn] as? String
        val createdDate = data[createdDateColumn] as? String
        val createType = data[createTypeColumn] as? Long
        if (id == null || statusDate == null || progressStatus == null ||
            dueDate == null || createType == null || createdDate == null ||
            task == null || member == null
        ) {
            return null
        }
        return TaskAssignment(
            id,
            ProgressStatus.fromKey(progressStatus.toInt()),
            instantConverter.toMain(statusDate),
            task,
            member,
            instantConverter.toMain(dueDate),
            instantConverter.toMain(createdDate),
            CreateType.fromKey(createType.toInt())
        )
    }
}