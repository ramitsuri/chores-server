package com.ramitsuri

import java.time.Duration
import java.time.Instant

object Constants {
    const val DB_DRIVER_TEST = "org.sqlite.JDBC"
    const val DB_URL_TEST = "jdbc:sqlite:data/test.db"
    const val JWT_REALM = "Chores"
    const val JWT_AUTH_CONFIG_BASE = "auth_base"
    val TOKEN_EXPIRATION: Duration = Duration.ofDays(30)
}