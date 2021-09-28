package com.ramitsuri.repeater

import com.ramitsuri.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskRepeaterTest: BaseRepeaterTest() {

    private val dispatcher = Dispatchers.Default
    private val baseInstant = Instant.ofEpochMilli(1614618000000) // Mon Mar 01 2021 17:00:00 UTC

    @Before
    fun setUp() {
        taskRepeater =
            TaskRepeater(
                tasksRepository,
                membersRepository,
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
        val taskDueDateTime = baseInstant.plusSeconds(3600)
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
        val taskDueDateTime = baseInstant.minusSeconds(3600)
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
        val taskDueDateTime = baseInstant
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
        val taskDueDateTime = baseInstant
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
    fun testStart_shouldNotAddAssignments_ifAssignmentsExistAndRunTimeLessThanMostRecentDueTime() {
        val taskDueDateTime = baseInstant
        val runDateTime = ZonedDateTime.ofInstant(baseInstant.minusSeconds(1), zoneId)
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
                createdDate = taskDueDateTime,
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
        val taskDueDateTime = baseInstant
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
                createdDate = taskDueDateTime,
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
    fun testStart_shouldAddNewAssignmentWithSameMemberId_ifAssignmentsExistAndRotateMemberFalse() {
        val taskDueDateTime = baseInstant
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
                createdDate = taskDueDateTime,
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
        val taskDueDateTime = baseInstant
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
                createdDate = taskDueDateTime,
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
        val taskDueDateTime = baseInstant
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
                createdDate = taskDueDateTime,
                taskId = task.id,
                memberId = member1.id
            )

            // Act
            taskRepeater.start(runDateTime, zoneId)
            val addedTaskAssignment = taskAssignmentsRepository.get()[1]

            // Assert
            assertEquals(baseInstant.plusSeconds(12 * 3600), addedTaskAssignment.dueDateTime)
        }
    }

    /*  @Test
      fun testStart_shouldAddOneNewAssignment_ifTaskHasNoAssignments() {
          runBlocking {
              addBasic()
              addTask("Task1")
              addAssignment()
              val addDateTime = ZonedDateTime.now(zoneId)
              taskRepeater.start(addDateTime, zoneId)
              val taskAssignments = taskAssignmentsRepository.get()
              assertEquals(1, taskAssignments.size)
          }
      }

      @Test
      fun testStart_shouldAddOneNewAssignment_ifTaskHasAssignmentsWithOldEnoughCreateDate_days() {
          val addDateTime = ZonedDateTime.now(zoneId)
          val previousCreateDate = addDateTime.minusDays(2).toInstant()
          setup(
              taskAssignment = TaskAssignment(
                  "",
                  ProgressStatus.TODO,
                  Instant.now(),
                  "",
                  "",
                  previousCreateDate,
                  CreateType.MANUAL
              )
          )
          runBlocking {
              taskRepeater.start(addDateTime, zoneId)
              val taskAssignments = taskAssignmentsRepository.get()
              assertEquals(2, taskAssignments.size)
          }
      }

      @Test
      fun testStart_shouldAddOneNewAssignment_ifTaskHasAssignmentsWithOldEnoughCreateDate_hours() {
          val addDateTime = ZonedDateTime.now(zoneId)
          val previousCreateDate = addDateTime.minusHours(2).toInstant()
          setup(
              taskAssignment = TaskAssignment(
                  "",
                  ProgressStatus.TODO,
                  Instant.now(),
                  "",
                  "",
                  previousCreateDate,
                  CreateType.MANUAL
              ),
              taskRepeatUnit = RepeatUnit.HOUR
          )
          runBlocking {
              taskRepeater.start(addDateTime, zoneId)
              val taskAssignments = taskAssignmentsRepository.get()
              assertEquals(2, taskAssignments.size)
          }
      }

      @Test
      fun testStart_shouldSetNextMember_ifTaskHasAssignments() {
          val addDateTime = ZonedDateTime.now(zoneId)
          val previousCreateDate = addDateTime.minusDays(2).toInstant()
          setup(
              taskAssignment = TaskAssignment(
                  "",
                  ProgressStatus.TODO,
                  Instant.now(),
                  "",
                  "",
                  previousCreateDate,
                  CreateType.MANUAL
              )
          )
          runBlocking {
              taskRepeater.start(addDateTime, zoneId)
              val taskAssignments = taskAssignmentsRepository.get()
              assertNotEquals(taskAssignments[0]!!.memberId, taskAssignments[1]!!.memberId)
              assertEquals(2, taskAssignments.size)
          }
      }*/

    private suspend fun addMember(name: String) {
        membersRepository.add(name, Instant.now())
    }

    private suspend fun addHouseAndMemberAssignments(name: String) {
        val members = membersRepository.get()
        val house = housesRepository.add(name, members[0].id, Instant.now(), ActiveStatus.ACTIVE)
        for (member in members) {
            memberAssignmentsRepository.add(member.id, house!!.id)
        }
    }

    private suspend fun addTask(
        name: String,
        dueDateTime: Instant,
        repeatValue: Int,
        repeatUnit: RepeatUnit,
        memberId: String,
        rotateMember: Boolean
    ) {
        val house = housesRepository.get().first()
        tasksRepository.add(
            name,
            name,
            dueDateTime,
            repeatValue,
            repeatUnit,
            house.id,
            memberId,
            rotateMember,
            Instant.now()
        )
    }

    private suspend fun addAssignment(dueDate: Instant, createdDate: Instant, taskId: String, memberId: String) {
        taskAssignmentsRepository.add(
            ProgressStatus.TODO,
            Instant.now(),
            taskId,
            memberId,
            dueDate,
            createdDate,
            CreateType.AUTO
        )
    }

    private suspend fun addBasic() {
        addMember("Member1")
        addMember("Member2")
        addHouseAndMemberAssignments("House1")
    }

    /*private fun setup(
        taskRepeatValue: Int = 1,
        taskRepeatUnit: RepeatUnit = RepeatUnit.DAY,
        taskAssignment: TaskAssignment? = null
    ) {
        runBlocking {
            val member1 = membersRepository.add("Member1", Instant.now())
            val member2 = membersRepository.add("Member2", Instant.now())
            val house1 = housesRepository.add("House1", member1!!.id, Instant.now(), ActiveStatus.ACTIVE)
            memberAssignmentsRepository.add(member1!!.id, house1!!.id)
            memberAssignmentsRepository.add(member2!!.id, house1!!.id)
            val task1 = tasksRepository.add("Task1", "Task1", taskRepeatValue, taskRepeatUnit, house1.id, Instant.now())
            if (taskAssignment != null) {
                taskAssignmentsRepository.add(
                    taskAssignment.progressStatus,
                    taskAssignment.progressStatusDate,
                    task1!!.id,
                    member1.id,
                    taskAssignment.createdDate,
                    taskAssignment.createType
                )
            }
        }
    }*/
}