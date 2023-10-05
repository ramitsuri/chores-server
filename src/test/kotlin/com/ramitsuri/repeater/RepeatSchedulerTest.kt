package com.ramitsuri.repeater

import com.ramitsuri.events.SystemEventService
import com.ramitsuri.testutils.TestRunTimeLogsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class RepeatSchedulerTest : BaseRepeaterTest() {
    private val dispatcher = Dispatchers.Default

    private lateinit var repeatScheduler: RepeatScheduler

    @Before
    fun setUp() {
        taskRepeater =
            TaskRepeater(
                tasksRepository,
                membersRepository,
                housesRepository,
                memberAssignmentsRepository,
                taskAssignmentsRepository,
                dispatcher
            )
    }

    @Test
    fun testScheduling_shouldRunAndDelayWithTenSeconds_ifRepeatTypeMinute() = runBlocking {
        setup()

        runBlocking {
            repeatScheduler.start()
        }
    }

    @Test
    fun testScheduling_shouldRunAndDelayWithTenSeconds_ifRepeatTypeMinutes() = runBlocking {
        setup()

        launch {
            delay(10.seconds)
            repeatScheduler.start()
        }
        repeatScheduler.start()
    }

    private fun CoroutineScope.setup() {
        val config = RepeatSchedulerConfig(
            SchedulerRepeatType.MINUTE,
            zoneId
        )
        repeatScheduler = RepeatScheduler(
            config = config,
            taskRepeater = taskRepeater,
            runTimeLogRepository = TestRunTimeLogsRepository(),
            clock = TestClock(Instant.parse("2023-01-01T12:00:00Z")),
            coroutineScope = this,
            ioDispatcher = dispatcher,
            eventService = SystemEventService(),
        )
    }
}

private class TestClock(private val toReturn: Instant) : Clock {
    override fun now(): Instant {
        return toReturn
    }
}