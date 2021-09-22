package com.ramitsuri.routes

import com.ramitsuri.models.Error
import com.ramitsuri.models.ErrorCode
import io.ktor.application.*
import io.ktor.http.*

abstract class Routes {
    abstract fun register(application: Application)

    protected fun getInvalidIdParamError(): Pair<HttpStatusCode, Error> {
        return Pair(
            HttpStatusCode.BadRequest,
            Error(
                ErrorCode.INVALID_REQUEST,
                "id query string parameter is required"
            )
        )
    }
}