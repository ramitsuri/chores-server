package com.ramitsuri.repeater

import com.ramitsuri.Constants
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.extensions.isLaterThan
import com.ramitsuri.models.RepeatSchedulerConfig
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

class RepeatScheduler(
    private val config: RepeatSchedulerConfig,
    private val taskRepeater: TaskRepeater
): Loggable {
    override val log = logger()

    suspend fun schedule() {
        log.info("Scheduling TaskRepeater with RepeatType: ${config.repeatType}}")
        var lastRunTime = ZonedDateTime.ofInstant(Constants.INSTANT_MIN, config.zoneId)
        while (true) {
            val nowDateTime = ZonedDateTime.now(config.zoneId).withNano(0)
            val canRun = nowDateTime.isLaterThan(lastRunTime.plus(config.repeatType.repeatDuration))
            val delayDuration = if (canRun) {
                lastRunTime = nowDateTime
                taskRepeater.start(nowDateTime, config.zoneId)
                config.repeatType.afterRunSleepDuration
            } else {
                config.repeatType.beforeRunSleepDuration
            }
            log.info("Delaying by $delayDuration")
            delay(delayDuration.toMillis())
        }
    }
}