package com.ramitsuri.routes

import com.ramitsuri.models.LoginParam
import com.ramitsuri.models.Token
import com.ramitsuri.plugins.JwtService
import com.ramitsuri.repository.interfaces.MembersRepository
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post

class LoginRoutes(
    jwtService: JwtService,
    membersRepository: MembersRepository
) : Routes(authenticationConfig = null) {

    override val path = "/login"

    override val routes: Route.() -> Unit = {
        post {
            val loginParam = call.receive<LoginParam>()
            val id = loginParam.id ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")
            val key = loginParam.key ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")

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