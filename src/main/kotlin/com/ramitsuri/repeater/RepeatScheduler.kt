package com.ramitsuri.repeater

import com.ramitsuri.events.Event
import com.ramitsuri.events.EventService
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.repository.interfaces.RunTimeLogsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class RepeatScheduler(
    private val config: RepeatSchedulerConfig,
    private val taskRepeater: TaskRepeater,
    private val runTimeLogRepository: RunTimeLogsRepository,
    private val clock: Clock,
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val eventService: EventService,
) : Loggable {
    override val log = logger()

    private val running = AtomicBoolean(false)
    private val resetRequestInProgress = AtomicBoolean(false)

    fun start() {
        coroutineScope.launch(ioDispatcher) {
            eventService.events.filter { it is Event.TaskNeedsAssignments }.collect {
                resetLastRunTime()
            }
        }
        coroutineScope.launch(ioDispatcher) {
            run()
        }
    }

    private suspend fun resetLastRunTime() {
        log.info("Resetting last run time")
        if (!resetRequestInProgress.compareAndSet(false, true)) {
            log.info("TaskRepeater reset request already in progress, ignoring")
            return
        }
        runTimeLogRepository.add(INSTANT_MIN.toJavaInstant())
        resetRequestInProgress.set(false)
    }

    private suspend fun run() {
        log.info("Scheduling TaskRepeater with RepeatType: ${config.repeatDuration}}")
        if (!running.compareAndSet(false, true)) {
            log.info("TaskRepeater already running, exiting")
            return
        }
        while (true) {
            val now = clock.now()
            val lastRunTime = runTimeLogRepository.get()?.toKotlinInstant() ?: INSTANT_MIN
            val durationSinceLastRun = now - lastRunTime
            if (durationSinceLastRun > config.repeatDuration) {
                taskRepeater.add(ZonedDateTime.ofInstant(now.toJavaInstant(), config.zoneId), config.zoneId)
                runTimeLogRepository.add(now.toJavaInstant())
            }
            delay(1.minutes)
        }
    }

    companion object {
        // 2021-01-01 00:00:00 UTC
        private val INSTANT_MIN: Instant = Instant.fromEpochMilliseconds(1609459200000)
    }
}

data class RepeatSchedulerConfig(
    val repeatDuration: Duration,
    val zoneId: ZoneId
) {
    companion object {
        fun ofOneHour(zoneId: ZoneId = ZoneId.of("UTC")): RepeatSchedulerConfig {
            return RepeatSchedulerConfig(
                repeatDuration = 1.hours,
                zoneId = zoneId
            )
        }

        fun ofOneMinute(zoneId: ZoneId = ZoneId.of("UTC")): RepeatSchedulerConfig {
            return RepeatSchedulerConfig(
                repeatDuration = 1.minutes,
                zoneId = zoneId
            )
        }
    }
}