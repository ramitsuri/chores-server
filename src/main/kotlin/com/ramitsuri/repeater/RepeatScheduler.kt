package com.ramitsuri.repeater

import com.ramitsuri.Constants
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.extensions.isLaterThan
import com.ramitsuri.models.RepeatSchedulerConfig
import com.ramitsuri.repository.interfaces.RunTimeLogsRepository
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

class RepeatScheduler(
    private val config: RepeatSchedulerConfig,
    private val taskRepeater: TaskRepeater,
    private val runTimeLogRepository: RunTimeLogsRepository
) : Loggable {
    override val log = logger()

    suspend fun schedule() {
        log.info("Scheduling TaskRepeater with RepeatType: ${config.repeatType}}")
        while (true) {
            val lastRunTime =
                ZonedDateTime.ofInstant(runTimeLogRepository.get() ?: Constants.INSTANT_MIN, config.zoneId)
            val nowDateTime = ZonedDateTime.now(config.zoneId)
            val canRun = nowDateTime.isLaterThan(lastRunTime.plus(config.repeatType.repeatDuration))
            log.info("Can run: $canRun")
            val delayDuration = if (canRun) {
                runTimeLogRepository.add(nowDateTime.toInstant())
                taskRepeater.start(nowDateTime, config.zoneId)
                config.repeatType.afterRunSleepDuration
            } else {
                config.repeatType.beforeRunSleepDuration
            }
            delay(delayDuration.toMillis())
        }
    }
}