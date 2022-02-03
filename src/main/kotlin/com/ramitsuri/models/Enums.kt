package com.ramitsuri.models

import java.time.Duration

enum class RepeatUnit(val key: Int) {
    NONE(0),
    DAY(1),
    WEEK(2),
    MONTH(3),
    HOUR(4),
    YEAR(5),
    ON_COMPLETE(6); // Repeat as soon as a task is completed

    companion object {
        fun fromKey(key: Int): RepeatUnit {
            for (value in values()) {
                if (value.key == key) {
                    return value
                }
            }
            return NONE
        }
    }
}

enum class ProgressStatus(val key: Int) {
    UNKNOWN(0),
    TODO(1),
    IN_PROGRESS(2),
    DONE(3);

    companion object {
        fun fromKey(key: Int): ProgressStatus {
            for (value in values()) {
                if (value.key == key) {
                    return value
                }
            }
            return UNKNOWN
        }
    }
}

enum class ActiveStatus(val key: Int) {
    UNKNOWN(0),
    ACTIVE(1),
    INACTIVE(2);

    companion object {
        fun fromKey(key: Int): ActiveStatus {
            for (value in values()) {
                if (value.key == key) {
                    return value
                }
            }
            return UNKNOWN
        }
    }
}

enum class CreateType(val key: Int) {
    UNKNOWN(0),
    MANUAL(1),
    AUTO(2);

    companion object {
        fun fromKey(key: Int): CreateType {
            for (value in values()) {
                if (value.key == key) {
                    return value
                }
            }
            return UNKNOWN
        }
    }
}

enum class ErrorCode(val key: Int) {
    UNKNOWN(0),
    INVALID_REQUEST(1),
    NOT_FOUND(2),
    INTERNAL_ERROR(3);

    companion object {
        fun fromKey(key: Int): ErrorCode {
            for (value in values()) {
                if (value.key == key) {
                    return value
                }
            }
            return UNKNOWN
        }
    }
}

enum class SchedulerRepeatType(
    val repeatDuration: Duration,
    val warmUpStartDuration: Duration
) {
    MINUTE(
        repeatDuration = Duration.ofMinutes(1),
        warmUpStartDuration = Duration.ofMinutes(1).minusSeconds(10)
    ),
    HOUR(
        repeatDuration = Duration.ofHours(1),
        warmUpStartDuration = Duration.ofHours(1).minusSeconds(30)
    ),
    DAY(
        repeatDuration = Duration.ofDays(1),
        warmUpStartDuration = Duration.ofDays(1).minusSeconds(30)
    )
}