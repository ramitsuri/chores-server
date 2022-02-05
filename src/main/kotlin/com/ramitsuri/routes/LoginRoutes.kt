package com.ramitsuri.routes

import com.ramitsuri.models.Token
import com.ramitsuri.plugins.JwtService
import com.ramitsuri.repository.interfaces.MembersRepository
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

class LoginRoutes(
    jwtService: JwtService,
    membersRepository: MembersRepository
) : Routes(authenticationConfig = null) {

    override val path = "/login"

    override val routes: Route.() -> Unit = {
        post {
            val loginParams = call.receive<Parameters>()
            val id = loginParams[ID_PARAM] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")
            val key = loginParams[KEY_PARAM] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")

            val authenticated = membersRepository.getAuthenticated(id, key)
            if (authenticated != null) {
                val authToken = jwtService.generateToken(authenticated)
                call.respond(HttpStatusCode.OK, Token(authToken))
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Unauthorized user")
            }
        }
    }

    companion object {
        private const val ID_PARAM = "id"
        private const val KEY_PARAM = "key"
    }
}