package com.ramitsuri.pushmessage

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.CreateType
import com.ramitsuri.models.House
import com.ramitsuri.models.Member
import com.ramitsuri.models.MemberAssignment
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.PushMessageToken
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import com.ramitsuri.models.TaskAssignment
import com.ramitsuri.models.TaskAssignmentDto
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import com.ramitsuri.repository.interfaces.TaskAssignmentInsert
import com.ramitsuri.testutils.BaseNeedsDatabaseTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertTrue

class PushMessagePayloadGeneratorTest : BaseNeedsDatabaseTest() {
    private lateinit var pushMessagePayloadGenerator: PushMessagePayloadGenerator
    private lateinit var now: Instant
    private lateinit var members: List<Member>
    private lateinit var houses: List<House>
    private lateinit var memberAssignments: List<MemberAssignment>
    private lateinit var tasks: List<Task>
    private lateinit var taskAssignments: List<TaskAssignment>
    private lateinit var pushMessageTokens: List<PushMessageToken>

    @Test
    fun getPushMessagesForTaskT1_shouldHaveTokensForAllDevicesOfAllMembersOfHouseOfTaskT1() = runBlocking {
        // Arrange
        setup(now = Instant.parse("2023-09-12T12:00:00Z"))

        // Act
        val pushMessages = pushMessagePayloadGenerator
            .getForTasks(tasks.filter { it.name == "T1" }.map { it.id }, now)

        // Assert
        assertEquals(5, pushMessages.size)
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM3D1" })
    }

    @Test
    fun getPushMessagesForTaskT2_shouldHaveTokensForAllDevicesOfAllMembersOfHouseOfTaskT2() = runBlocking {
        // Arrange
        setup(now = Instant.parse("2023-09-12T12:00:00Z"))

        // Act
        val pushMessages = pushMessagePayloadGenerator
            .getForTasks(tasks.filter { it.name == "T2" }.map { it.id }, now)

        // Assert
        assertEquals(4, pushMessages.size)
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D2" })
    }

    @Test
    fun getPushMessagesForTaskAssignment_ifCompletedByAssignedMember() = runTest {
        // Arrange
        setup(now = Instant.parse("2023-09-12T12:00:00Z"))

        // Act
        val taskAssignmentsToEdit = taskAssignments
            .filter {
                it.task.name == "T1" && it.member.name == "M1"
            }
            .map {
                TaskAssignmentDto(
                    id = it.id,
                    progressStatus = ProgressStatus.DONE.key,
                    progressStatusDate = now
                )
            }
        val requesterMemberId = members.first { it.name == "M1" }.id
        testAppContainer.taskAssignmentsRepository.edit(taskAssignmentsToEdit, requesterMemberId)
        val pushMessages = pushMessagePayloadGenerator
            .getForTaskAssignments(taskAssignmentsToEdit.map { it.id }, now)

        // Assert
        assertEquals(5, pushMessages.size)
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM3D1" })
        assertTrue(pushMessages.all { it.payload.doneByOthers.isEmpty() && it.payload.wontDoByOthers.isEmpty() })
    }

    @Test
    fun getPushMessagesForTaskAssignment_ifCompletedByNotAssignedMember() = runTest {
        // Arrange
        setup(now = Instant.parse("2023-09-12T12:00:00Z"))

        // Act
        val taskAssignmentsToEdit = taskAssignments
            .filter {
                it.task.name == "T1" && it.member.name == "M1"
            }
            .map {
                TaskAssignmentDto(
                    id = it.id,
                    progressStatus = ProgressStatus.DONE.key,
                    progressStatusDate = now
                )
            }
        val requesterMemberId = members.first { it.name == "M2" }.id
        testAppContainer.taskAssignmentsRepository.edit(taskAssignmentsToEdit, requesterMemberId)
        val pushMessages = pushMessagePayloadGenerator
            .getForTaskAssignments(taskAssignmentsToEdit.map { it.id }, now)

        // Assert
        assertEquals(5, pushMessages.size)
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM3D1" })
        val (m1Messages, otherMessages) = pushMessages.partition { it.recipientToken.startsWith("TokenM1") }
        assertTrue(m1Messages.all { it.payload.doneByOthers.isNotEmpty() && it.payload.wontDoByOthers.isEmpty() })
        assertTrue(otherMessages.all { it.payload.doneByOthers.isEmpty() && it.payload.wontDoByOthers.isEmpty() })
    }

    @Test
    fun getPushMessagesForTaskAssignment_ifWontDoByAssignedMember() = runTest {
        // Arrange
        setup(now = Instant.parse("2023-09-12T12:00:00Z"))

        // Act
        val taskAssignmentsToEdit = taskAssignments
            .filter {
                it.task.name == "T1" && it.member.name == "M1"
            }
            .map {
                TaskAssignmentDto(
                    id = it.id,
                    progressStatus = ProgressStatus.WONT_DO.key,
                    progressStatusDate = now
                )
            }
        val requesterMemberId = members.first { it.name == "M1" }.id
        testAppContainer.taskAssignmentsRepository.edit(taskAssignmentsToEdit, requesterMemberId)
        val pushMessages = pushMessagePayloadGenerator
            .getForTaskAssignments(taskAssignmentsToEdit.map { it.id }, now)

        // Assert
        assertEquals(5, pushMessages.size)
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM3D1" })
        assertTrue(pushMessages.all { it.payload.doneByOthers.isEmpty() && it.payload.wontDoByOthers.isEmpty() })
    }

    @Test
    fun getPushMessagesForTaskAssignment_ifWontDoByNotAssignedMember() = runTest {
        // Arrange
        setup(now = Instant.parse("2023-09-12T12:00:00Z"))

        // Act
        val taskAssignmentsToEdit = taskAssignments
            .filter {
                it.task.name == "T1" && it.member.name == "M1"
            }
            .map {
                TaskAssignmentDto(
                    id = it.id,
                    progressStatus = ProgressStatus.WONT_DO.key,
                    progressStatusDate = now
                )
            }
        val requesterMemberId = members.first { it.name == "M2" }.id
        testAppContainer.taskAssignmentsRepository.edit(taskAssignmentsToEdit, requesterMemberId)
        val pushMessages = pushMessagePayloadGenerator
            .getForTaskAssignments(taskAssignmentsToEdit.map { it.id }, now)

        // Assert
        assertEquals(5, pushMessages.size)
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM1D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D1" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM2D2" })
        assertNotNull(pushMessages.firstOrNull { it.recipientToken == "TokenM3D1" })
        val (m1Messages, otherMessages) = pushMessages.partition { it.recipientToken.startsWith("TokenM1") }
        assertTrue(m1Messages.all { it.payload.doneByOthers.isEmpty() && it.payload.wontDoByOthers.isNotEmpty() })
        assertTrue(otherMessages.all { it.payload.doneByOthers.isEmpty() && it.payload.wontDoByOthers.isEmpty() })
    }

    private fun setup(now: Instant) {
        pushMessagePayloadGenerator = PushMessagePayloadGenerator(
            tasksRepository = testAppContainer.tasksRepository,
            taskAssignmentsRepository = testAppContainer.taskAssignmentsRepository,
            memberAssignmentsRepository = testAppContainer.memberAssignmentsRepository,
            pushMessageTokenRepository = testAppContainer.pushMessageTokenRepository
        )
        this.now = now
        val createDateTime = this.now
        val dueDateTime = LocalDateTime.parse("2023-09-20T12:00:00")
        runBlocking {
            val m1 = testAppContainer.membersRepository.add("M1", createDateTime)!!
            val m2 = testAppContainer.membersRepository.add("M2", createDateTime)!!
            val m3 = testAppContainer.membersRepository.add("M3", createDateTime)!!
            members = testAppContainer.membersRepository.get()

            val h1 = testAppContainer.housesRepository.add("H1", m1.id, createDateTime, ActiveStatus.ACTIVE)!!
            val h2 = testAppContainer.housesRepository.add("H2", m1.id, createDateTime, ActiveStatus.ACTIVE)!!
            houses = testAppContainer.housesRepository.get()

            /*
             * House H1 has Members M1, M2, M3
             */
            testAppContainer.memberAssignmentsRepository.add(memberId = m1.id, houseId = h1.id)
            testAppContainer.memberAssignmentsRepository.add(memberId = m2.id, houseId = h1.id)
            testAppContainer.memberAssignmentsRepository.add(memberId = m3.id, houseId = h1.id)

            /*
             * House H2 has Members M1, M2
             */
            testAppContainer.memberAssignmentsRepository.add(memberId = m1.id, houseId = h2.id)
            testAppContainer.memberAssignmentsRepository.add(memberId = m2.id, houseId = h2.id)

            memberAssignments = testAppContainer.memberAssignmentsRepository.get()

            /*
             * Task T1 is in House H1, has 3 Task Assignments assigned to Members M1, M2, M3
             */
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
                        dueDateTime = dueDateTime,
                        createdDateTime = createDateTime,
                        createType = CreateType.AUTO
                    ),
                    TaskAssignmentInsert(
                        progressStatus = ProgressStatus.TODO,
                        progressStatusDateTime = createDateTime,
                        taskId = t1.id,
                        memberId = m2.id,
                        dueDateTime = dueDateTime,
                        createdDateTime = createDateTime,
                        createType = CreateType.AUTO
                    ),
                    TaskAssignmentInsert(
                        progressStatus = ProgressStatus.TODO,
                        progressStatusDateTime = createDateTime,
                        taskId = t1.id,
                        memberId = m3.id,
                        dueDateTime = dueDateTime,
                        createdDateTime = createDateTime,
                        createType = CreateType.AUTO
                    )
                )
            )

            /*
             * Task T2 is in House H2, has 2 Task Assignments assigned to Members M1, M2
             */
            val t2 = testAppContainer.tasksRepository.add(
                name = "T2",
                description = "T2",
                dueDate = dueDateTime,
                repeatValue = 1,
                repeatUnit = RepeatUnit.HOUR,
                repeatEndDate = null,
                houseId = h2.id,
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
                        dueDateTime = dueDateTime,
                        createdDateTime = createDateTime,
                        createType = CreateType.AUTO
                    ),
                    TaskAssignmentInsert(
                        progressStatus = ProgressStatus.TODO,
                        progressStatusDateTime = createDateTime,
                        taskId = t2.id,
                        memberId = m2.id,
                        dueDateTime = dueDateTime,
                        createdDateTime = createDateTime,
                        createType = CreateType.AUTO
                    )
                )
            )
            tasks = testAppContainer.tasksRepository.get()
            taskAssignments =
                testAppContainer.taskAssignmentsRepository.get(TaskAssignmentFilter(onlyActiveAndPausedHouse = false))

            /*
             * Member M1 and M2 have 2 devices, M3 has 1 device
             */
            testAppContainer.pushMessageTokenRepository.addOrReplace(
                memberId = m1.id,
                deviceId = UUID.randomUUID().toString(),
                token = "TokenM1D1",
                addedDateTime = createDateTime
            )
            testAppContainer.pushMessageTokenRepository.addOrReplace(
                memberId = m2.id,
                deviceId = UUID.randomUUID().toString(),
                token = "TokenM2D1",
                addedDateTime = createDateTime
            )
            testAppContainer.pushMessageTokenRepository.addOrReplace(
                memberId = m3.id,
                deviceId = UUID.randomUUID().toString(),
                token = "TokenM3D1",
                addedDateTime = createDateTime
            )
            testAppContainer.pushMessageTokenRepository.addOrReplace(
                memberId = m1.id,
                deviceId = UUID.randomUUID().toString(),
                token = "TokenM1D2",
                addedDateTime = createDateTime
            )
            testAppContainer.pushMessageTokenRepository.addOrReplace(
                memberId = m2.id,
                deviceId = UUID.randomUUID().toString(),
                token = "TokenM2D2",
                addedDateTime = createDateTime
            )
            pushMessageTokens = testAppContainer.pushMessageTokenRepository.getOfMembers(members.map { it.id }, now)
        }
    }
}