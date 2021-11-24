package com.ramitsuri.routes

import com.ramitsuri.data.Migration
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

class MigrationRoutes(private val migration: Migration) : Routes() {
    override fun register(application: Application) {
        application.routing {
            route("/migration") {

                // Get Counts
                get("counts") {
                    call.respond(migration.counts())
                }

                // Get Counts
                get("run") {
                    call.respond(migration.run())
                }
            }
        }
    }
}