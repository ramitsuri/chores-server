package com.ramitsuri.routes

import com.ramitsuri.Constants
import com.ramitsuri.plugins.JwtService
import com.ramitsuri.plugins.configureSecurity
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals

class RoutesTest {
    private class AuthedRoutes : Routes() {
        override val path = "/authed"
        override val routes: Route.() -> Unit = { get { call.respondText("authed") } }
    }

    private class OpenRoutes : Routes(authenticationConfig = null) {
        override val path = "/open"
        override val routes: Route.() -> Unit = { post { call.respondText("open") } }
    }

    @Test
    fun register_shouldMountRoutesUnderTheirPath() = testApplication {
        application {
            configureSecurity(
                JwtService("issuer", mockk(relaxed = true), Duration.ofDays(1), "secret"),
                Constants.JWT_REALM,
                Constants.JWT_AUTH_CONFIG_BASE
            )
            AuthedRoutes().register(this)
            OpenRoutes().register(this)
        }

        assertEquals(HttpStatusCode.OK, client.post("/open").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/authed").status)
        assertEquals(HttpStatusCode.NotFound, client.post("/").status)
    }
}
