package com.ramitsuri.repository.local

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.repository.interfaces.TaskAssignmentInsert
import com.ramitsuri.testutils.BaseNeedsDatabaseTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class LocalTaskAssignmentsRepositoryTest : BaseNeedsDatabaseTest() {
    private var dueDateTime = LocalDateTime.parse("2023-09-20T12:00:00")

    private lateinit var repository: LocalTaskAssignmentsRepository

    @Test
    fun getMostRecentForTask() = runTest {
        setup()

        val taskT1 = testAppContainer.tasksRepository.get().first { it.name == "T1" }
        val mostRecentTaskAssignment = testAppContainer.taskAssignmentsRepository.getMostRecentForTask(taskT1.id)
        assertNotNull(mostRecentTaskAssignment!!)
        assertEquals(dueDateTime.plusMinutes(3), mostRecentTaskAssignment.dueDateTime)

        val taskT2 = testAppContainer.tasksRepository.get().first { it.name == "T2" }
        assertNull(testAppContainer.taskAssignmentsRepository.getMostRecentForTask(taskT2.id))
    }

    @Test
    fun testAddReturnsInsertedIds() = runTest {
        setup()

        // Should manually check for example with event service that ids are being returned
        val taskT1 = testAppContainer.tasksRepository.get().first { it.name == "T1" }

        repository.add(
            listOf(
                TaskAssignmentInsert(
                    progressStatus = ProgressStatus.TODO,
                    progressStatusDateTime = Instant.parse("2023-09-30T12:00:00Z"),
                    taskId = taskT1.id,
                    memberId = UUID.randomUUID().toString(),
                    dueDateTime = dueDateTime,
                    createdDateTime = Instant.parse("2023-09-30T12:00:00Z"),
                    createType = CreateType.AUTO
                ),
                TaskAssignmentInsert(
                    progressStatus = ProgressStatus.TODO,
                    progressStatusDateTime = Instant.parse("2023-09-30T12:00:00Z"),
                    taskId = taskT1.id,
                    memberId = UUID.randomUUID().toString(),
                    dueDateTime = dueDateTime,
                    createdDateTime = Instant.parse("2023-09-30T12:00:00Z"),
                    createType = CreateType.AUTO
                ),
                TaskAssignmentInsert(
                    progressStatus = ProgressStatus.TODO,
                    progressStatusDateTime = Instant.parse("2023-09-30T12:00:00Z"),
                    taskId = taskT1.id,
                    memberId = UUID.randomUUID().toString(),
                    dueDateTime = dueDateTime,
                    createdDateTime = Instant.parse("2023-09-30T12:00:00Z"),
                    createType = CreateType.AUTO
                ),
                TaskAssignmentInsert(
                    progressStatus = ProgressStatus.TODO,
                    progressStatusDateTime = Instant.parse("2023-09-30T12:00:00Z"),
                    taskId = taskT1.id,
                    memberId = UUID.randomUUID().toString(),
                    dueDateTime = dueDateTime,
                    createdDateTime = Instant.parse("2023-09-30T12:00:00Z"),
                    createType = CreateType.AUTO
                ),
                TaskAssignmentInsert(
                    progressStatus = ProgressStatus.TODO,
                    progressStatusDateTime = Instant.parse("2023-09-30T12:00:00Z"),
                    taskId = taskT1.id,
                    memberId = UUID.randomUUID().toString(),
                    dueDateTime = dueDateTime,
                    createdDateTime = Instant.parse("2023-09-30T12:00:00Z"),
                    createType = CreateType.AUTO
                )
            )
        )
    }

    private fun setup() {
        val createDateTime = Instant.parse("2023-09-30T12:00:00Z")
        repository = testAppContainer.taskAssignmentsRepository
        runBlocking {
            val m1 = testAppContainer.membersRepository.add("M1", createDateTime)!!
            val h1 = testAppContainer.housesRepository.add("H1", m1.id, createDateTime, ActiveStatus.ACTIVE)!!
            testAppContainer.memberAssignmentsRepository.add(memberId = m1.id, houseId = h1.id)

            val t1 = testAppContainer.tasksRepository.add(
                name = "T1",
                description = "T1",
                dueDate = dueDateTime,
                repeatValue = 1,
                repeatUnit = RepeatUnit.HOUR,
                repeatEndDate = null,
                houseId = h1.id,
                memberId = m1.id,
                rotateMember = false,
                createdDate = createDateTime,
                status = ActiveStatus.ACTIVE
            )!!
            testAppContainer.taskAssignmentsRepository.add(
                listOf(
                    TaskAssignmentInsert(
                        progressStatus = ProgressStatus.TODO,
                        progressStatusDateTime = createDateTime,
                        taskId = t1.id,
                        memberId = m1.id,
                        dueDateTime = t1.dueDateTime.plusMinutes(1),
                        createdDateTime = createDateTime,
                        createType = CreateType.AUTO
                    ),
                    TaskAssignmentInsert(
                        progressStatus = ProgressStatus.TODO,
                        progressStatusDateTime = createDateTime,
                        taskId = t1.id,
                        memberId = m1.id,
                        dueDateTime = t1.dueDateTime.plusMinutes(2),
                        createdDateTime = createDateTime,
                        createType = CreateType.AUTO
                    ),
                    TaskAssignmentInsert(
                        progressStatus = ProgressStatus.TODO,
                        progressStatusDateTime = createDateTime,
                        taskId = t1.id,
                        memberId = m1.id,
                        dueDateTime = t1.dueDateTime.plusMinutes(3),
                        createdDateTime = createDateTime,
                        createType = CreateType.AUTO
                    )
                )
            )

            // No assignments for t2
            val t2 = testAppContainer.tasksRepository.add(
                name = "T2",
                description = "T2",
                dueDate = dueDateTime,
                repeatValue = 1,
                repeatUnit = RepeatUnit.HOUR,
                repeatEndDate = null,
                houseId = h1.id,
                memberId = m1.id,
                rotateMember = false,
                createdDate = createDateTime,
                status = ActiveStatus.ACTIVE
            )!!
        }
    }
}