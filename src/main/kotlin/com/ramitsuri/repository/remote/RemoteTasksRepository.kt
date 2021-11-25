package com.ramitsuri.repository.remote

import com.google.cloud.firestore.Firestore
import com.ramitsuri.data.Converter
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.extensions.wait
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import com.ramitsuri.repository.interfaces.HousesRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import java.time.Instant
import java.util.*

class RemoteTasksRepository(
    private val collection: String,
    private val db: Firestore,
    private val housesRepository: HousesRepository,
    private val instantConverter: Converter<Instant, String>,
    private val uuidConverter: Converter<String, UUID>
): TasksRepository, Loggable {
    override val log = logger()

    private val idColumn = "id"
    private val nameColumn = "name"
    private val descriptionColumn = "description"
    private val dueDateColumn = "dueDate"
    private val repeatValueColumn = "repeatValue"
    private val repeatUnitColumn = "repeatUnit"
    private val houseIdColumn = "houseId"
    private val memberIdColumn = "memberId"
    private val rotateMemberColumn = "rotateMember"
    private val createdDateColumn = "createdDate"

    override suspend fun add(
        name: String,
        description: String,
        dueDate: Instant,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        houseId: String,
        memberId: String,
        rotateMember: Boolean,
        createdDate: Instant
    ): Task? {
        if (housesRepository.get(houseId) == null) {
            log.warning("HouseId doesn't belong to a house")
            return null
        }
        val id = uuidConverter.toMain(UUID.randomUUID())
        val map = mapOf<String, Any>(
            idColumn to id,
            nameColumn to name,
            descriptionColumn to description,
            dueDateColumn to instantConverter.toStorage(dueDate),
            repeatValueColumn to repeatValue,
            repeatUnitColumn to repeatUnit.key,
            houseIdColumn to houseId,
            memberIdColumn to memberId,
            rotateMemberColumn to rotateMember,
            createdDateColumn to instantConverter.toStorage(createdDate)
        )
        val result = try {
            db.collection(collection)
                .document(id)
                .set(map).wait()
        } catch (e: Exception) {
            null
        }

        return if (result == null) {
            log.warning("Cannot get Task")
            null
        } else {
            Task(id, name, description, dueDate, repeatValue, repeatUnit, houseId, memberId, rotateMember, createdDate)
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

    override suspend fun edit(
        id: String,
        name: String,
        description: String,
        dueDate: Instant,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        rotateMember: Boolean
    ): Int {
        val result = try {
            val map = mapOf<String, Any>(
                nameColumn to name,
                descriptionColumn to description,
                dueDateColumn to instantConverter.toStorage(dueDate),
                repeatValueColumn to repeatValue,
                repeatUnitColumn to repeatUnit.key,
                rotateMemberColumn to rotateMember,
            )
            db.collection(collection).document(id).update(map)
        } catch (e: Exception) {
            null
        }
        return if (result == null) {
            0
        } else {
            1
        }
    }

    override suspend fun get(): List<Task> {
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
            val list = mutableListOf<Task>()
            for (document in result.documents) {
                val task = toTask(document.data)
                if (task != null) {
                    list.add(task)
                }
            }
            list
        }
    }

    override suspend fun get(id: String): Task? {
        val result = try {
            db.collection(collection)
                .document(id)
                .get()
                .wait()
        } catch (e: Exception) {
            null
        }
        result?.data?.let {
            return toTask(it)
        }
        return null
    }

    private fun toTask(data: Map<String, Any>): Task? {
        val id = data[idColumn] as? String
        val name = data[nameColumn] as? String
        val description = data[descriptionColumn] as? String
        val dueDate = data[dueDateColumn] as? String
        val repeatValue = data[repeatValueColumn] as? Long
        val repeatUnit = data[repeatUnitColumn] as? Long
        val houseId = data[houseIdColumn] as? String
        val memberId = data[memberIdColumn] as? String
        val rotateMember = data[rotateMemberColumn] as? Boolean
        val createdDate = data[createdDateColumn] as? String
        if (id == null || name == null || description == null || dueDate == null ||
            repeatValue == null || repeatUnit == null || houseId == null ||
            memberId == null || rotateMember == null || createdDate == null
        ) {
            return null
        }
        return Task(
            id,
            name,
            description,
            instantConverter.toMain(dueDate),
            repeatValue.toInt(),
            RepeatUnit.fromKey(repeatUnit.toInt()),
            houseId,
            memberId,
            rotateMember,
            instantConverter.toMain(createdDate),
        )
    }
}