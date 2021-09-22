package com.ramitsuri.extensions

import java.time.ZonedDateTime

fun ZonedDateTime.isLaterThan(time: ZonedDateTime): Boolean {
    return this.isAfter(time) || this.isEqual(time)
}