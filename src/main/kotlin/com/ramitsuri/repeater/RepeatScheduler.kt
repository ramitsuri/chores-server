package com.ramitsuri.repeater

import com.ramitsuri.Constants
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.extensions.isLaterThan
import com.ramitsuri.models.RepeatSchedulerConfig
import com.ramitsuri.repository.interfaces.RunTimeLogsRepository
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean

class RepeatScheduler(
    private val config: RepeatSchedulerConfig,
    private val taskRepeater: TaskRepeater,
    private val runTimeLogRepository: RunTimeLogsRepository
) : Loggable {
    override val log = logger()

    private val running = AtomicBoolean(false)

    suspend fun schedule() {
        log.info("Scheduling TaskRepeater with RepeatType: ${config.repeatType}}")
        if (!running.compareAndSet(false, true)) {
            log.info("Already running, exiting")
            return
        }
        while (true) {
            val now = ZonedDateTime.now(config.zoneId)
            val lastRunTime =
                ZonedDateTime.ofInstant(runTimeLogRepository.get() ?: Constants.INSTANT_MIN, config.zoneId)
            val absoluteStartTime = lastRunTime.plus(config.repeatType.repeatDuration)

            val warmUpStartTime = lastRunTime.plus(config.repeatType.warmUpStartDuration)
            val isInWarmUpPeriod = now.isLaterThan(warmUpStartTime) && now.isBefore(absoluteStartTime)

            val delayDuration = if (now.isBefore(warmUpStartTime)) {
                // Before warm up period, wait until warm up period starts
                Duration.between(now, warmUpStartTime)
            } else if (isInWarmUpPeriod) {
                // Is in warm up period. Keep delaying 1 second until repeater can run
                Duration.ofSeconds(1)
            } else {
                runTimeLogRepository.add(now.withNano(0).toInstant())
                taskRepeater.start(now, config.zoneId)
                // Repeater ran. Should now delay until warm up start period begins.
                config.repeatType.warmUpStartDuration
            }
            delay(delayDuration.toMillis())
        }
    }
}