package com.ramitsuri.routes

import com.ramitsuri.utils.DummyRepository
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

class DummyRoutes(val repository: DummyRepository) : Routes() {
    override val path = "/dummy-add"

    override val routes: Route.() -> Unit = {
        get {
            repository.add()
            call.respond("Dummy data added")
        }
    }
}