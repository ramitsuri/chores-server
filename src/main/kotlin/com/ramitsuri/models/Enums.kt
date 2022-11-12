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
    DONE(3),
    WONT_DO(4);

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
    INACTIVE(2), // This status means that the entity with this status is no longer being used
    PAUSED(3);

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
    INTERNAL_ERROR(3),
    INVALID_TOKEN(4),
    EMPTY_TOKEN(5),
    MISSING_AUTHORIZATION(6),
    EXPIRED_TOKEN(7);

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

enum class Access(val key: Int) {
    NONE(0),
    READ_HOUSE_WRITE_OWN(1),
    READ_HOUSE_WRITE_HOUSE(2),
    READ_ALL_WRITE_ALL(3);

    companion object {
        fun fromKey(key: Int): Access {
            for (value in values()) {
                if (value.key == key) {
                    return value
                }
            }
            return NONE
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