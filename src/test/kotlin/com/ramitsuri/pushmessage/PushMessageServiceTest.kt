package com.ramitsuri.pushmessage

import com.ramitsuri.Constants
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.TaskAssignmentDto
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import com.ramitsuri.repository.interfaces.TaskAssignmentInsert
import com.ramitsuri.testutils.BaseNeedsDatabaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PushMessageServiceTest : BaseNeedsDatabaseTest() {

    @Test
    fun testPushMessageSentOnAssignmentsAdded() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        DatabaseFactory.init(
            url = Constants.DB_URL_TEST,
            driver = Constants.DB_DRIVER_TEST,
            dispatcher = testDispatcher
        )
        val dispatchedData = mutableListOf<Pair<String, Map<String, String>>>()

        val dispatcher = object : PushMessageDispatcher {
            override fun sendData(deviceToken: String, data: Map<String, String>) {
                dispatchedData.add(Pair(deviceToken, data))
            }

            override fun sendNotification(deviceToken: String, notificationContent: NotificationContent) {}
        }

        val createDateTime = Instant.parse("2023-09-30T12:00:00Z")
        val dueDateTime = LocalDateTime.parse("2023-09-30T12:00:00")

        val m1 = testAppContainer.membersRepository.add("M1", createDateTime)!!
        val m2 = testAppContainer.membersRepository.add("M2", createDateTime)!!
        val h1 = testAppContainer.housesRepository.add("H1", m1.id, createDateTime, ActiveStatus.ACTIVE)!!
        testAppContainer.memberAssignmentsRepository.add(memberId = m1.id, houseId = h1.id)
        testAppContainer.memberAssignmentsRepository.add(memberId = m2.id, houseId = h1.id)

        testAppContainer.pushMessageTokenRepository.addOrReplace(m1.id, UUID.randomUUID().toString(), "Token1", Instant.now())
        testAppContainer.pushMessageTokenRepository.addOrReplace(m2.id, UUID.randomUUID().toString(), "Token2", Instant.now())

        val task = testAppContainer.tasksRepository.add(
            name = "Task 1",
            description = "Desc",
            dueDate = dueDateTime,
            repeatValue = 1,
            repeatUnit = RepeatUnit.DAY,
            repeatEndDate = null,
            houseId = h1.id,
            memberId = m1.id,
            rotateMember = false,
            createdDate = createDateTime,
            status = ActiveStatus.ACTIVE
        )!!

        val generator = PushMessagePayloadGenerator(
            tasksRepository = testAppContainer.tasksRepository,
            taskAssignmentsRepository = testAppContainer.taskAssignmentsRepository,
            memberAssignmentsRepository = testAppContainer.memberAssignmentsRepository,
            pushMessageTokenRepository = testAppContainer.pushMessageTokenRepository
        )

        PushMessageService(
            pushMessageDispatcher = dispatcher,
            pushMessagePayloadGenerator = generator,
            coroutineScope = backgroundScope,
            ioDispatcher = testDispatcher,
            eventService = testAppContainer.eventService
        )
        testScheduler.runCurrent()

        val assignmentInsert = TaskAssignmentInsert(
            progressStatus = ProgressStatus.TODO,
            progressStatusDateTime = createDateTime,
            taskId = task.id,
            memberId = m1.id,
            dueDateTime = dueDateTime,
            createdDateTime = createDateTime,
            createType = CreateType.AUTO
        )

        testAppContainer.taskAssignmentsRepository.add(listOf(assignmentInsert))

        testScheduler.advanceUntilIdle()
        assertEquals(2, dispatchedData.size)
        val tokens = dispatchedData.map { it.first }
        assertTrue(tokens.contains("Token1"))
        assertTrue(tokens.contains("Token2"))
    }

    @Test
    fun testPushMessageSentOnAssignmentsUpdatedAndTransferred() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        DatabaseFactory.init(
            url = Constants.DB_URL_TEST,
            driver = Constants.DB_DRIVER_TEST,
            dispatcher = testDispatcher
        )
        val dispatchedData = mutableListOf<Pair<String, Map<String, String>>>()

        val dispatcher = object : PushMessageDispatcher {
            override fun sendData(deviceToken: String, data: Map<String, String>) {
                dispatchedData.add(Pair(deviceToken, data))
            }

            override fun sendNotification(deviceToken: String, notificationContent: NotificationContent) {}
        }

        val createDateTime = Instant.parse("2023-09-30T12:00:00Z")
        val dueDateTime = LocalDateTime.parse("2023-09-30T12:00:00")

        val m1 = testAppContainer.membersRepository.add("M1", createDateTime)!!
        val m2 = testAppContainer.membersRepository.add("M2", createDateTime)!!
        val h1 = testAppContainer.housesRepository.add("H1", m1.id, createDateTime, ActiveStatus.ACTIVE)!!
        testAppContainer.memberAssignmentsRepository.add(memberId = m1.id, houseId = h1.id)
        testAppContainer.memberAssignmentsRepository.add(memberId = m2.id, houseId = h1.id)

        testAppContainer.pushMessageTokenRepository.addOrReplace(m1.id, UUID.randomUUID().toString(), "Token1", Instant.now())
        testAppContainer.pushMessageTokenRepository.addOrReplace(m2.id, UUID.randomUUID().toString(), "Token2", Instant.now())

        val task = testAppContainer.tasksRepository.add(
            name = "Task 1",
            description = "Desc",
            dueDate = dueDateTime,
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
                    taskId = task.id,
                    memberId = m1.id,
                    dueDateTime = dueDateTime,
                    createdDateTime = createDateTime,
                    createType = CreateType.AUTO
                )
            )
        )

        val createdAssignments = testAppContainer.taskAssignmentsRepository.get(
            TaskAssignmentFilter(onlyActiveAndPausedHouse = false)
        )
        val createdAssignmentId = createdAssignments.first().id

        val generator = PushMessagePayloadGenerator(
            tasksRepository = testAppContainer.tasksRepository,
            taskAssignmentsRepository = testAppContainer.taskAssignmentsRepository,
            memberAssignmentsRepository = testAppContainer.memberAssignmentsRepository,
            pushMessageTokenRepository = testAppContainer.pushMessageTokenRepository
        )

        PushMessageService(
            pushMessageDispatcher = dispatcher,
            pushMessagePayloadGenerator = generator,
            coroutineScope = backgroundScope,
            ioDispatcher = testDispatcher,
            eventService = testAppContainer.eventService
        )
        testScheduler.runCurrent()

        // Transfer assignment
        val dto = TaskAssignmentDto(
            id = createdAssignmentId,
            progressStatus = ProgressStatus.TODO.key,
            shouldTransfer = true
        )
        testAppContainer.taskAssignmentsRepository.edit(listOf(dto), m1.id)

        testScheduler.advanceUntilIdle()
        assertEquals(2, dispatchedData.size)
        val tokens = dispatchedData.map { it.first }
        assertTrue(tokens.contains("Token1"))
        assertTrue(tokens.contains("Token2"))
    }

    @Test
    fun testPushMessageSentOnTaskEdited() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        DatabaseFactory.init(
            url = Constants.DB_URL_TEST,
            driver = Constants.DB_DRIVER_TEST,
            dispatcher = testDispatcher
        )
        val dispatchedData = mutableListOf<Pair<String, Map<String, String>>>()

        val dispatcher = object : PushMessageDispatcher {
            override fun sendData(deviceToken: String, data: Map<String, String>) {
                dispatchedData.add(Pair(deviceToken, data))
            }

            override fun sendNotification(deviceToken: String, notificationContent: NotificationContent) {}
        }

        val createDateTime = Instant.parse("2023-09-30T12:00:00Z")
        val dueDateTime = LocalDateTime.parse("2023-09-30T12:00:00")

        val m1 = testAppContainer.membersRepository.add("M1", createDateTime)!!
        val m2 = testAppContainer.membersRepository.add("M2", createDateTime)!!
        val h1 = testAppContainer.housesRepository.add("H1", m1.id, createDateTime, ActiveStatus.ACTIVE)!!
        testAppContainer.memberAssignmentsRepository.add(memberId = m1.id, houseId = h1.id)
        testAppContainer.memberAssignmentsRepository.add(memberId = m2.id, houseId = h1.id)

        testAppContainer.pushMessageTokenRepository.addOrReplace(m1.id, UUID.randomUUID().toString(), "Token1", Instant.now())
        testAppContainer.pushMessageTokenRepository.addOrReplace(m2.id, UUID.randomUUID().toString(), "Token2", Instant.now())

        val task = testAppContainer.tasksRepository.add(
            name = "Task 1",
            description = "Desc",
            dueDate = dueDateTime,
            repeatValue = 1,
            repeatUnit = RepeatUnit.DAY,
            repeatEndDate = null,
            houseId = h1.id,
            memberId = m1.id,
            rotateMember = false,
            createdDate = createDateTime,
            status = ActiveStatus.ACTIVE
        )!!

        val generator = PushMessagePayloadGenerator(
            tasksRepository = testAppContainer.tasksRepository,
            taskAssignmentsRepository = testAppContainer.taskAssignmentsRepository,
            memberAssignmentsRepository = testAppContainer.memberAssignmentsRepository,
            pushMessageTokenRepository = testAppContainer.pushMessageTokenRepository
        )

        PushMessageService(
            pushMessageDispatcher = dispatcher,
            pushMessagePayloadGenerator = generator,
            coroutineScope = backgroundScope,
            ioDispatcher = testDispatcher,
            eventService = testAppContainer.eventService
        )
        testScheduler.runCurrent()

        // Edit task
        testAppContainer.tasksRepository.edit(
            id = task.id,
            name = "Updated Task Name",
            description = "Desc",
            dueDate = dueDateTime,
            repeatValue = 1,
            repeatUnit = RepeatUnit.DAY,
            repeatEndDate = null,
            rotateMember = false,
            status = ActiveStatus.ACTIVE
        )

        testScheduler.advanceUntilIdle()
        assertEquals(2, dispatchedData.size)
        val tokens = dispatchedData.map { it.first }
        assertTrue(tokens.contains("Token1"))
        assertTrue(tokens.contains("Token2"))
    }
}
