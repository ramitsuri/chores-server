package com.ramitsuri.repeater

import com.ramitsuri.events.Event
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.testutils.TestEventsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TaskRepeaterTest : BaseRepeaterTest() {

    private val dispatcher = Dispatchers.Default
    private val baseInstant = Instant.ofEpochMilli(1614618000000) // Mon Mar 01 2021 17:00:00 UTC
    private val baseLocalDateTime = LocalDateTime.ofInstant(baseInstant, zoneId) // Mon Mar 01 2021 17:00:00 UTC
    private lateinit var eventsService: TestEventsService

    @Before
    fun setUp() {
        eventsService = TestEventsService()
        taskRepeater =
            TaskRepeater(
                eventsService,
                tasksRepository,
                membersRepository,
                housesRepository,
                memberAssignmentsRepository,
                taskAssignmentsRepository,
                dispatcher
            )
    }

    @After
    fun tearDown() {
        runBlocking {
            membersRepository.delete()
            housesRepository.delete()
            memberAssignmentsRepository.delete()
            tasksRepository.delete()
            taskAssignmentsRepository.delete()
        }
    }

    @Test
    fun testStart_shouldNotAddAnything_ifRepositoriesEmpty() {
        runBlocking {
            val addDateTime = ZonedDateTime.now(zoneId)
            taskRepeater.start(addDateTime, zoneId)
            val taskAssignments = taskAssignmentsRepository.get()
            assertTrue(taskAssignments.isEmpty())
        }
    }

    @Test
    fun testStart_shouldAddFirstAssignment_ifNoAssignmentsYetAndRunTimeBeforeDueTime() {
        val taskDueDateTime = baseLocalDateTime.plusSeconds(3600)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(1, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldAddFirstAssignment_ifNoAssignmentsYetAndRunTimeAfterDueTime() {
        val taskDueDateTime = baseLocalDateTime.minusSeconds(3600)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(1, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldAddFirstAssignment_ifNoAssignmentsYetAndRunTimeSameAsDueTime() {
        val taskDueDateTime = baseLocalDateTime
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(1, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldHaveCorrectValuesFromTask_ifNoAssignmentsYet() {
        val taskDueDateTime = baseLocalDateTime
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )

            // Act
            val task1 = tasksRepository.get()[0]
            taskRepeater.start(runDateTime, zoneId)
            val taskAssignment = taskAssignmentsRepository.get()[0]

            // Assert
            assertEquals(ProgressStatus.TODO, taskAssignment.progressStatus)
            assertEquals(CreateType.AUTO, taskAssignment.createType)
            assertEquals(task1, taskAssignment.task)
            assertEquals(member1, taskAssignment.member)
            assertEquals(taskDueDateTime, taskAssignment.dueDateTime)
        }
    }

    @Test
    fun testStart_shouldNotAddAssignments_ifAssignmentsExistAndDurationBetweenRunTimeAndDueDateTimeMoreThan7Days() {
        val taskDueDateTime = baseLocalDateTime.plusDays(7).plusSeconds(1)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )
            val task = tasksRepository.get()[0]

            addAssignment(
                dueDate = taskDueDateTime,
                createdDate = baseInstant,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(1, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldAddNewAssignment_ifAssignmentsExistAndRunTimeAfterOrEqualMostRecentDueTime() {
        val taskDueDateTime = baseLocalDateTime
        val runDateTime = ZonedDateTime.ofInstant(baseInstant.plusSeconds(24 * 3600), zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )
            val task = tasksRepository.get()[0]

            addAssignment(
                dueDate = taskDueDateTime,
                createdDate = baseInstant,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(2, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldAddNewAssignment_ifTaskRepeatOnCompleteAndMostRecentAssignmentCompleted() {
        val taskDueDateTime = baseLocalDateTime
        val runDateTime = ZonedDateTime.ofInstant(baseInstant.minusSeconds(1), zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.ON_COMPLETE,
                member1.id,
                rotateMember = false
            )
            val task = tasksRepository.get()[0]

            addAssignment(
                progressStatus = ProgressStatus.DONE,
                dueDate = taskDueDateTime,
                createdDate = baseInstant,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(2, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldAddNewAssignmentOnlyOnce_ifTaskRepeatOnCompleteAndMostRecentAssignmentCompletedAndRepeaterRunMultipleTimes() {
        val taskDueDateTime = baseLocalDateTime
        val runDateTime = ZonedDateTime.ofInstant(baseInstant.plusSeconds(1), zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.ON_COMPLETE,
                member1.id,
                rotateMember = false
            )
            val task = tasksRepository.get()[0]

            addAssignment(
                progressStatus = ProgressStatus.DONE,
                dueDate = taskDueDateTime,
                createdDate = baseInstant,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(2, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldAddNewAssignmentWithNewMember_ifTaskRepeatOnCompleteAndMostRecentAssignmentCompletedAndRotateMemberTrue() {
        val taskDueDateTime = baseLocalDateTime
        val runDateTime = ZonedDateTime.ofInstant(baseInstant.plusSeconds(1), zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.ON_COMPLETE,
                member1.id,
                rotateMember = true
            )
            val task = tasksRepository.get()[0]

            addAssignment(
                progressStatus = ProgressStatus.DONE,
                dueDate = taskDueDateTime,
                createdDate = baseInstant,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            val assignments = taskAssignmentsRepository.get()
            assertNotEquals(assignments[0].member.id, assignments[1].member.id)
        }
    }

    @Test
    fun testStart_shouldAddNewAssignmentWithSameMemberId_ifAssignmentsExistAndRotateMemberFalse() {
        val taskDueDateTime = baseLocalDateTime
        val runDateTime = ZonedDateTime.ofInstant(baseInstant.plusSeconds(24 * 3600), zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )
            val task = tasksRepository.get()[0]

            addAssignment(
                dueDate = taskDueDateTime,
                createdDate = baseInstant,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(member1, taskAssignmentsRepository.get()[1].member)
        }
    }

    @Test
    fun testStart_shouldAddNewAssignmentWithDifferentMemberId_ifAssignmentsExistAndRotateMemberTrue() {
        val taskDueDateTime = baseLocalDateTime
        val runDateTime = ZonedDateTime.ofInstant(baseInstant.plusSeconds(24 * 3600), zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            val member2 = membersRepository.get()[1]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = true
            )
            val task = tasksRepository.get()[0]

            addAssignment(
                dueDate = taskDueDateTime,
                createdDate = baseInstant,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(member2, taskAssignmentsRepository.get()[1].member)
        }
    }

    @Test
    fun testStart_shouldAddNewAssignmentWithCorrectDueDateTime() {
        val taskDueDateTime = baseLocalDateTime
        val runDateTime = ZonedDateTime.ofInstant(baseInstant.plusSeconds(24 * 3600), zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 12,
                RepeatUnit.HOUR,
                member1.id,
                rotateMember = true
            )
            val task = tasksRepository.get()[0]
            addAssignment(
                dueDate = taskDueDateTime,
                createdDate = baseInstant,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)
            val addedTaskAssignment = taskAssignmentsRepository.get()[1]

            // Assert
            assertEquals(baseLocalDateTime.plusSeconds(12 * 3600), addedTaskAssignment.dueDateTime)
        }
    }

    @Test
    fun testStart_shouldNotAddNewAssignment_ifHouseInactive() {
        val taskDueDateTime = baseLocalDateTime.plusSeconds(3600)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic(houseStatus = ActiveStatus.INACTIVE)
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(0, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldNotAddNewAssignment_ifHouseStatusUnknown() {
        val taskDueDateTime = baseLocalDateTime.plusSeconds(3600)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic(houseStatus = ActiveStatus.UNKNOWN)
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(0, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldAddNewAssignmentWithStatusWontDo_ifHouseStatusPaused() {
        val taskDueDateTime = baseLocalDateTime.plusSeconds(3600)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic(houseStatus = ActiveStatus.PAUSED)
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(1, taskAssignmentsRepository.get().size)
            assertEquals(ProgressStatus.WONT_DO, taskAssignmentsRepository.get()[0].progressStatus)
        }
    }

    @Test
    fun testStart_shouldAddNewAssignment_ifTaskStatusActive() {
        val taskDueDateTime = baseLocalDateTime.plusSeconds(3600)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic(houseStatus = ActiveStatus.ACTIVE)
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false,
                status = ActiveStatus.ACTIVE
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(1, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldNotAddNewAssignment_ifTaskStatusUnknown() {
        val taskDueDateTime = baseLocalDateTime.plusSeconds(3600)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic(houseStatus = ActiveStatus.ACTIVE)
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false,
                status = ActiveStatus.UNKNOWN
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(0, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldNotAddNewAssignment_ifTaskStatusInactive() {
        val taskDueDateTime = baseLocalDateTime.plusSeconds(3600)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic(houseStatus = ActiveStatus.ACTIVE)
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false,
                status = ActiveStatus.INACTIVE
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(0, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldNotAddNewAssignment_ifTaskStatusPaused() {
        val taskDueDateTime = baseLocalDateTime.plusSeconds(3600)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic(houseStatus = ActiveStatus.ACTIVE)
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false,
                status = ActiveStatus.PAUSED
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(0, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldNotAddNewAssignment_ifRepeatEndDateBeforeDueDate() {
        val taskDueDateTime = baseLocalDateTime.plusSeconds(3600)
        val runDateTime = ZonedDateTime.ofInstant(baseInstant, zoneId)
        runBlocking {
            // Arrange
            addBasic(houseStatus = ActiveStatus.ACTIVE)
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false,
                status = ActiveStatus.ACTIVE,
                repeatEndDateTime = taskDueDateTime.minusSeconds(1)
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(0, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldNotAddNewAssignment_ifRepeatEndDateBeforeDueDate2() {
        val taskDueDateTime = baseLocalDateTime
        // Run date time is 2 hours after task due date time, repetition is every hour. So, should've added 2
        // new assignments but room to add only 1 new assignment because repeatEndDateTime is 59 mins after
        // original due date time so only the first assignment can be created
        val runDateTime = ZonedDateTime.ofInstant(baseInstant.plusSeconds(2 * 60 * 60), zoneId)
        runBlocking {
            // Arrange
            addBasic(houseStatus = ActiveStatus.ACTIVE)
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.HOUR,
                member1.id,
                rotateMember = false,
                status = ActiveStatus.ACTIVE,
                repeatEndDateTime = taskDueDateTime.plusMinutes(59)
            )
            val task = tasksRepository.get()[0]
            addAssignment(
                dueDate = taskDueDateTime,
                createdDate = baseInstant,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(1, taskAssignmentsRepository.get().size)
        }
    }

    @Test
    fun testStart_shouldNotifyEventsService_ifNewTasksAdded() {
        val taskDueDateTime = baseLocalDateTime
        val runDateTime = ZonedDateTime.ofInstant(baseInstant.plusSeconds(24 * 3600), zoneId)
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 12,
                RepeatUnit.HOUR,
                member1.id,
                rotateMember = true
            )
            val task = tasksRepository.get()[0]
            addAssignment(
                dueDate = taskDueDateTime,
                createdDate = baseInstant,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)

            // Assert
            assertEquals(1, eventsService.getEvents().size)
            assertTrue(eventsService.getEvents()[0] is Event.AssignmentsAdded)
        }
    }

    @Test
    fun testStart_shouldAddAssignmentsWithSameDueDateTime_ifDSTChanges() {
        val taskDueDateTime = LocalDateTime.parse("2022-09-28T07:00")
        runBlocking {
            // Arrange
            addBasic()
            val member1 = membersRepository.get()[0]
            addTask(
                name = "Task1",
                dueDateTime = taskDueDateTime,
                repeatValue = 1,
                RepeatUnit.DAY,
                member1.id,
                rotateMember = false
            )
            val taskId = tasksRepository.get()[0].id
            // An assignment already there before DST switch happens on 11-06
            addAssignment(
                dueDate = LocalDateTime.parse("2022-11-05T07:00"),
                createdDate = baseInstant,
                taskId = taskId,
                memberId = member1.id
            )

            // Act
            val runTime = LocalDateTime.parse("2022-11-06T08:00").atZone(zoneId)
            taskRepeater.start(runTime, zoneId)

            // Assert
            val addedAssignment = taskAssignmentsRepository.get()[1]
            assertEquals(LocalTime.of(7, 0), addedAssignment.dueDateTime.toLocalTime())
        }
    }

    private suspend fun addMember(name: String) {
        membersRepository.add(name, Instant.now())
    }

    private suspend fun addHouseAndMemberAssignments(
        houseName: String,
        houseStatus: ActiveStatus = ActiveStatus.ACTIVE
    ) {
        val members = membersRepository.get()
        val house = housesRepository.add(houseName, members[0].id, Instant.now(), houseStatus)
        for (member in members) {
            memberAssignmentsRepository.add(member.id, house!!.id)
        }
    }

    private suspend fun addTask(
        name: String,
        dueDateTime: LocalDateTime,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        memberId: String,
        rotateMember: Boolean,
        status: ActiveStatus = ActiveStatus.ACTIVE,
        repeatEndDateTime: LocalDateTime? = null,
    ) {
        val house = housesRepository.get().first()
        tasksRepository.add(
            name,
            name,
            dueDateTime,
            repeatValue,
            repeatUnit,
            repeatEndDateTime,
            house.id,
            memberId,
            rotateMember,
            Instant.now(),
            status
        )
    }

    private suspend fun addAssignment(
        dueDate: LocalDateTime,
        createdDate: Instant,
        taskId: String,
        memberId: String,
        progressStatus: ProgressStatus = ProgressStatus.TODO
    ) {
        taskAssignmentsRepository.add(
            progressStatus,
            Instant.now(),
            taskId,
            memberId,
            dueDate,
            createdDate,
            CreateType.AUTO
        )
    }

    private suspend fun addBasic(houseStatus: ActiveStatus = ActiveStatus.ACTIVE) {
        addMember("Member1")
        addMember("Member2")
        addHouseAndMemberAssignments("House1", houseStatus)
    }
}