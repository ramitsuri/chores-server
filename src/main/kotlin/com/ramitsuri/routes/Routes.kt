package com.ramitsuri.routes

import com.ramitsuri.Constants
import com.ramitsuri.models.Error
import com.ramitsuri.models.ErrorCode
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.routing.*

abstract class Routes(private val authenticationConfig: String? = Constants.JWT_AUTH_CONFIG_BASE) {
    fun register(application: Application) {
        application.routing {
            if (authenticationConfig != null) {
                authenticate(authenticationConfig) {
                    route(path) {
                        routes()
                    }
                }
            } else {
                route(path) {
                    routes()
                }
            }
        }
    }

    protected val invalidIdParamError: Pair<HttpStatusCode, Error> =
        Pair(
            HttpStatusCode.BadRequest,
            Error(
                ErrorCode.INVALID_REQUEST,
                "id query string parameter is required"
            )
        )

    protected val invalidTokenError: Pair<HttpStatusCode, Error> =
        Pair(
            HttpStatusCode.Unauthorized,
            Error(
                ErrorCode.INVALID_TOKEN,
                "authorization token is not valid"
            )
        )

    abstract val routes: Route.() -> Unit
    abstract val path: String

}