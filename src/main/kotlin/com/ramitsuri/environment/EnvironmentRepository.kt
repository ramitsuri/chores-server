package com.ramitsuri.environment

import java.util.*

class EnvironmentRepository {
    private val properties = Properties()

    init {
        val rootPath = object {}.javaClass.getResourceAsStream("/env.properties")
        if (rootPath != null) {
            properties.load(rootPath)
        }
    }

    fun getDbUrl() = System.getenv("DB_URL") ?: properties.getProperty("DB_URL") ?: ""

    fun getDbDriver() = System.getenv("DB_DRIVER") ?: properties.getProperty("DB_DRIVER") ?: ""

    fun getDbUsername() = System.getenv("DB_USERNAME") ?: properties.getProperty("DB_USERNAME") ?: ""

    fun getDbPassword() = System.getenv("DB_PASSWORD") ?: properties.getProperty("DB_PASSWORD") ?: ""

    fun getJwtSecret() = System.getenv("JWT_SECRET") ?: properties.getProperty("JWT_SECRET") ?: "jwt_secret"

    fun getJwtIssuer() = System.getenv("JWT_ISSUER") ?: properties.getProperty("JWT_ISSUER") ?: "jwt_issuer"
}
