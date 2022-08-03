package com.ramitsuri.routes

import com.ramitsuri.models.AccessResult
import com.ramitsuri.plugins.getMemberId
import com.ramitsuri.repository.access.SyncAccessController
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

class SyncRoutes(private val syncAccessController: SyncAccessController) : Routes() {
    override val path = "/sync"

    override val routes: Route.() -> Unit = {
        get {
            val requesterMemberId = this.context.getMemberId() ?: return@get call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )
            when (val accessResult = syncAccessController.get(requesterMemberId)) {
                is AccessResult.Success -> {
                    call.respond(accessResult.data)
                }
                is AccessResult.Failure -> {
                    call.respond(HttpStatusCode.Unauthorized, "Not sufficient access")
                }
            }
        }
    }
}