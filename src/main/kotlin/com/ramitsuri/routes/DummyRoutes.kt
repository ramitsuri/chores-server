package com.ramitsuri.routes

import com.ramitsuri.utils.DummyRepository
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

class DummyRoutes(val repository: DummyRepository): Routes() {
    override fun register(application: Application) {
        application.routing {
            route("/dummy-add") {
                get {
                    repository.add()
                    call.respond("Dummy data added")
                }
            }
        }
    }
}