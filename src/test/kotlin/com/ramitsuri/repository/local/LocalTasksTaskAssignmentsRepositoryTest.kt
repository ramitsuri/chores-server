package com.ramitsuri.repository.local

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.repository.interfaces.TaskAssignmentInsert
import com.ramitsuri.testutils.BaseNeedsDatabaseTest
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class LocalTasksTaskAssignmentsRepositoryTest : BaseNeedsDatabaseTest() {

    private lateinit var repository: LocalTasksTaskAssignmentsRepository

    @Test
    fun testGetReturnsOnlyTasksWithAssignmentInLastOneYear() = runTest {
        val now = Clock.System.now()
        val nowInstant = Instant.ofEpochMilli(now.toEpochMilliseconds())
        val createDateTime = nowInstant
        repository = testAppContainer.tasksTaskAssignmentsRepository

        val m1 = testAppContainer.membersRepository.add("M1", createDateTime)!!
        val h1 = testAppContainer.housesRepository.add("H1", m1.id, createDateTime, ActiveStatus.ACTIVE)!!
        testAppContainer.memberAssignmentsRepository.add(memberId = m1.id, houseId = h1.id)

        // Task 1: Assignment within past 1 year (30 days ago)
        val t1Due = now.minus(30.days).toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime()
        val t1 = testAppContainer.tasksRepository.add(
            name = "T1",
            description = "Task within 1 year",
            dueDate = t1Due,
            repeatValue = 1,
            repeatUnit = RepeatUnit.DAY,
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
                    dueDateTime = t1Due,
                    createdDateTime = createDateTime,
                    createType = CreateType.AUTO
                )
            )
        )

        // Task 2: Assignment older than 1 year (400 days ago)
        val t2Due = now.minus(400.days).toLocalDateTime(TimeZone.UTC).toJavaLocalDateTime()
        val t2 = testAppContainer.tasksRepository.add(
            name = "T2",
            description = "Task older than 1 year",
            dueDate = t2Due,
            repeatValue = 1,
            repeatUnit = RepeatUnit.DAY,
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
                    taskId = t2.id,
                    memberId = m1.id,
                    dueDateTime = t2Due,
                    createdDateTime = createDateTime,
                    createType = CreateType.AUTO
                )
            )
        )

        // Task 3: No assignments at all
        testAppContainer.tasksRepository.add(
            name = "T3",
            description = "Task with no assignments",
            dueDate = LocalDateTime.parse("2024-05-01T12:00:00"),
            repeatValue = 1,
            repeatUnit = RepeatUnit.DAY,
            repeatEndDate = null,
            houseId = h1.id,
            memberId = m1.id,
            rotateMember = false,
            createdDate = createDateTime,
            status = ActiveStatus.ACTIVE
        )!!

        val tasks = repository.get()
        assertEquals(1, tasks.size)
        assertEquals("T1", tasks[0].name)

        val houseTasks = repository.getForHouses(listOf(h1.id))
        assertEquals(1, houseTasks.size)
        assertEquals("T1", houseTasks[0].name)

        val otherHouseTasks = repository.getForHouses(listOf(UUID.randomUUID().toString()))
        assertTrue(otherHouseTasks.isEmpty())
    }
}
