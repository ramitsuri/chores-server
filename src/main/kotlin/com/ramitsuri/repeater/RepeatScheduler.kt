package com.ramitsuri.repeater

import com.ramitsuri.events.Event
import com.ramitsuri.events.EventService
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.repository.interfaces.RunTimeLogsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RepeatScheduler(
    private val config: RepeatSchedulerConfig,
    private val taskRepeater: TaskRepeater,
    private val runTimeLogRepository: RunTimeLogsRepository,
    private val clock: Clock,
    coroutineScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    eventService: EventService,
) : Loggable {
    override val log = logger()

    private val running = AtomicBoolean(false)
    private val restartRequestInProgress = AtomicBoolean(false)
    private val cancelable = AtomicBoolean(true)

    private var runningJob: Job? = null

    init {
        coroutineScope.launch(ioDispatcher) {
            eventService.events.filter { it is Event.TaskNeedsAssignments }.collect {
                restart()
            }
        }
    }

    suspend fun start() {
        log.info("Scheduling TaskRepeater with RepeatType: ${config.repeatType}}")
        if (!running.compareAndSet(false, true)) {
            log.info("TaskRepeater already running, exiting")
            return
        }

        restartRunJob()
    }

    private suspend fun restart() {
        log.info("Restarting TaskRepeater")
        if (!restartRequestInProgress.compareAndSet(false, true)) {
            log.info("TaskRepeater restart already in progress, ignoring request")
            return
        }
        runTimeLogRepository.add(INSTANT_MIN.toJavaInstant())
        running.set(false)
        restartRequestInProgress.set(false)

        start()
    }

    private suspend fun restartRunJob() {
        log.info("Restart run job: cancelable: ${cancelable.get()}")
        withContext(Dispatchers.IO) {
            while (!cancelable.get()) {
                log.info("Not cancelable. Will wait 1 second")
                delay(1.seconds)
            }
            runningJob?.cancel()
            runningJob = launch {
                while (true) {
                    val now = clock.now()
                    val lastRunTime = runTimeLogRepository.get()?.toKotlinInstant() ?: INSTANT_MIN
                    val durationSinceLastRun = now - lastRunTime
                    if (durationSinceLastRun > config.repeatType.repeatDuration) {
                        log.info("About to add via task repeater, setting cancelable false")
                        cancelable.set(false)
                        runTimeLogRepository.add(now.toJavaInstant())
                        log.info("Update run time log repo")
                        taskRepeater.add(ZonedDateTime.ofInstant(now.toJavaInstant(), config.zoneId), config.zoneId)
                        log.info("added via task repeater, setting cancelable true")
                        cancelable.set(true)
                        delay(config.repeatType.repeatDuration)
                    } else {
                        delay(config.repeatType.repeatDuration.minus(durationSinceLastRun))
                    }
                }
            }
        }
    }

    companion object {
        // 2021-01-01 00:00:00 UTC
        private val INSTANT_MIN: Instant = Instant.fromEpochMilliseconds(1609459200000)
    }
}

data class RepeatSchedulerConfig(
    val repeatType: SchedulerRepeatType,
    val zoneId: ZoneId
)

enum class SchedulerRepeatType(
    val repeatDuration: Duration,
) {
    MINUTE(
        repeatDuration = 1.minutes,
    ),
    HOUR(
        repeatDuration = 1.hours,
    ),
    DAY(
        repeatDuration = 1.days,
    )
}