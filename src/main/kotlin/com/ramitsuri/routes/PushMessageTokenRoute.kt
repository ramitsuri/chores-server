package com.ramitsuri.routes

import com.ramitsuri.models.PushMessageTokenDto
import com.ramitsuri.plugins.getMemberId
import com.ramitsuri.repository.interfaces.PushMessageTokenRepository
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.put

class PushMessageTokenRoute(private val repository: PushMessageTokenRepository) : Routes() {
    override val path: String = "/push-token"

    override val routes: Route.() -> Unit = {
        get {
            val requesterMemberId = this.context.getMemberId() ?: return@get call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )
            call.respond(repository.getForMember(requesterMemberId))
        }

        put {
            val requesterMemberId = this.context.getMemberId() ?: return@put call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )

            val pushMessageTokenDto = call.receive<PushMessageTokenDto>()
            val result = repository.addOrReplace(
                memberId = requesterMemberId,
                deviceId = pushMessageTokenDto.deviceId,
                token = pushMessageTokenDto.token
            )
            return@put if (result != null) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}