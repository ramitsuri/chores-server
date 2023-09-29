package com.ramitsuri.routes

import com.ramitsuri.models.PushMessageTokenDto
import com.ramitsuri.plugins.getMemberId
import com.ramitsuri.repository.interfaces.PushMessageTokenRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import java.time.Instant

class PushMessageTokenRoute(private val repository: PushMessageTokenRepository) : Routes() {
    override val path: String = "/push-token"

    override val routes: Route.() -> Unit = {
        put {
            val requesterMemberId = this.context.getMemberId() ?: return@put call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )

            val pushMessageTokenDto = call.receive<PushMessageTokenDto>()
            val result = repository.addOrReplace(
                memberId = requesterMemberId,
                deviceId = pushMessageTokenDto.deviceId,
                token = pushMessageTokenDto.token,
                addedDateTime = Instant.now(),
            )
            return@put if (result != null) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}