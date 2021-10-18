package com.ramitsuri.repeater

import com.ramitsuri.models.RepeatSchedulerConfig
import com.ramitsuri.models.SchedulerRepeatType
import com.ramitsuri.testutils.TestEventsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import java.time.Duration

class RepeatSchedulerTest: BaseRepeaterTest() {
    private val dispatcher = Dispatchers.Default

    private lateinit var repeatScheduler: RepeatScheduler

    @Before
    fun setUp() {
        taskRepeater =
            TaskRepeater(
                TestEventsService(),
                tasksRepository,
                membersRepository,
                memberAssignmentsRepository,
                taskAssignmentsRepository,
                dispatcher
            )
    }

    @Test
    fun testScheduling_shouldRunAndDelayWithTenSeconds_ifRepeatTypeMinute() {
        val config = RepeatSchedulerConfig(
            SchedulerRepeatType.MINUTE,
            zoneId
        )
        repeatScheduler = RepeatScheduler(config, taskRepeater)
        runBlocking {
            repeatScheduler.schedule()
        }
    }

    @Test
    fun shortensDelays() {
        val dispatcher = TestCoroutineDispatcher()
        val testScope = TestCoroutineScope(dispatcher)
        // Pass the same dispatcher to TaskRepeater
        testScope.runBlockingTest {
            // repeatScheduler.start()
        }
    }
}