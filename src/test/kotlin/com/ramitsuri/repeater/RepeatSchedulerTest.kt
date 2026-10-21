package com.ramitsuri.repeater

import com.ramitsuri.events.Event
import com.ramitsuri.events.SystemEventService
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.testutils.TestRunTimeLogsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import kotlin.time.Clock
import kotlin.time.toKotlinInstant

@OptIn(ExperimentalCoroutinesApi::class)
class RepeatSchedulerTest : BaseRepeaterTest() {

    private val eventService = SystemEventService()
    private val runTimeLogsRepository = TestRunTimeLogsRepository()
    private val baseInstant = Instant.ofEpochMilli(1614618000000) // Mon Mar 01 2021 17:00:00 UTC
    private val baseLocalDateTime = LocalDateTime.ofInstant(baseInstant, zoneId)

    @After
    fun tearDown() = runTest {
        membersRepository.delete()
        housesRepository.delete()
        memberAssignmentsRepository.delete()
        tasksRepository.delete()
        taskAssignmentsRepository.delete()
    }

    @Test
    fun testScheduling_shouldRunAndAddAssignments_ifLastRunTimeNotSet() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        taskRepeater = TaskRepeater(
            tasksRepository,
            membersRepository,
            housesRepository,
            memberAssignmentsRepository,
            taskAssignmentsRepository,
            dispatcher
        )

        // Arrange
        addBasic()
        val member1 = membersRepository.get()[0]
        addTask(
            name = "Task1",
            dueDateTime = baseLocalDateTime,
            repeatValue = 1,
            repeatUnit = RepeatUnit.DAY,
            memberId = member1.id,
            rotateMember = false
        )

        val repeatScheduler = RepeatScheduler(
            config = RepeatSchedulerConfig.ofOneMinute(zoneId),
            taskRepeater = taskRepeater,
            runTimeLogRepository = runTimeLogsRepository,
            clock = TestClock(baseInstant.toKotlinInstant()),
            coroutineScope = backgroundScope,
            ioDispatcher = dispatcher,
            eventService = eventService,
        )

        // Act
        repeatScheduler.start()

        // Assert
        assertEquals(1, taskAssignmentsRepository.get().size)
        assertNotNull(runTimeLogsRepository.get())
    }

    @Test
    fun testScheduling_shouldResetLastRunTimeAndRerun_ifTaskNeedsAssignmentsEventPosted() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        taskRepeater = TaskRepeater(
            tasksRepository,
            membersRepository,
            housesRepository,
            memberAssignmentsRepository,
            taskAssignmentsRepository,
            dispatcher
        )

        // Arrange
        addBasic()
        val member1 = membersRepository.get()[0]
        addTask(
            name = "Task1",
            dueDateTime = baseLocalDateTime,
            repeatValue = 1,
            repeatUnit = RepeatUnit.DAY,
            memberId = member1.id,
            rotateMember = false
        )

        // Set last run time to recent (same as baseInstant) so RepeatScheduler won't run initially for hourly config
        runTimeLogsRepository.add(baseInstant)

        val repeatScheduler = RepeatScheduler(
            config = RepeatSchedulerConfig.ofOneHour(zoneId),
            taskRepeater = taskRepeater,
            runTimeLogRepository = runTimeLogsRepository,
            clock = TestClock(baseInstant.toKotlinInstant()),
            coroutineScope = backgroundScope,
            ioDispatcher = dispatcher,
            eventService = eventService,
        )

        // Act
        repeatScheduler.start()

        // Initially no assignment added because lastRunTime == baseInstant
        assertEquals(0, taskAssignmentsRepository.get().size)

        // Post event that task needs assignments
        eventService.post(Event.TaskNeedsAssignments)

        // Advance test scheduler past delay(1.minutes)
        testScheduler.advanceTimeBy(60_001)

        // Assert: Event trigger resets last run time and causes TaskRepeater to add assignment
        assertEquals(1, taskAssignmentsRepository.get().size)
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

    private suspend fun addBasic(houseStatus: ActiveStatus = ActiveStatus.ACTIVE) {
        addMember("Member1")
        addMember("Member2")
        addHouseAndMemberAssignments("House1", houseStatus)
    }
}

private class TestClock(private val toReturn: kotlin.time.Instant) : Clock {
    override fun now(): kotlin.time.Instant {
        return toReturn
    }
}
