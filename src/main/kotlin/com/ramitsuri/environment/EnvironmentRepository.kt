package com.ramitsuri.environment

fun getDbUrl() = System.getenv("DB_URL") ?: ""

fun getDbDriver() = System.getenv("DB_DRIVER") ?: ""

fun getDbUsername() = System.getenv("DB_USERNAME") ?: ""

fun getDbPassword() = System.getenv("DB_PASSWORD") ?: ""