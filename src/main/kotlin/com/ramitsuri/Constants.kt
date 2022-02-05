package com.ramitsuri

import java.time.Duration
import java.time.Instant

object Constants {
    const val DB_URL = "jdbc:sqlite:data/data.db"
    const val DB_DRIVER = "org.sqlite.JDBC"
    const val DB_URL_IN_MEMORY = "jdbc:sqlite:file:test?mode=memory&cache=shared"
    const val DB_URL_TEST = "jdbc:sqlite:data/test.db"
    const val JWT_REALM = "Chores"
    const val JWT_AUTH_CONFIG_BASE = "auth_base"
    val INSTANT_MIN: Instant = Instant.ofEpochMilli(1609459200000) // 2021-01-01 00:00:00 UTC
    val TOKEN_EXPIRATION: Duration = Duration.ofDays(30)
}