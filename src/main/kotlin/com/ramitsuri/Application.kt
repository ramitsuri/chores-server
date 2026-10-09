package com.ramitsuri

import com.ramitsuri.di.AppContainer
import com.ramitsuri.plugins.configureSecurity
import com.ramitsuri.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.application.plugin
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingRoot

fun main() {
    val appContainer = AppContainer()
    val environment = appContainer.environment
    appContainer.getDatabase().init(
        environment.getDbUrl(),
        environment.getDbDriver(),
        environment.getDbUsername(),
        environment.getDbPassword()
    )
    if (environment.addDummyData()) {
        appContainer.dummyDataProvider.setup()
    }
    appContainer.getTaskScheduler().start()
    embeddedServer(appContainer.getApplicationEngine(), configure = {
        connector {
            // App Engine provides the port to listen on via PORT
            port = System.getenv("PORT")?.toIntOrNull() ?: 8081
        }
        connectionGroupSize = 2
        workerGroupSize = 5
        callGroupSize = 10
    }) {
        configureSecurity(appContainer.getJwtService(), Constants.JWT_REALM, Constants.JWT_AUTH_CONFIG_BASE)
        for (routes in appContainer.getRoutes()) {
            routes.register(this)
        }
        configureSerialization()
        // Uncomment to log routes
        //logRoutes()
    }.start(wait = true)
}

private fun Application.logRoutes() {
    val root = plugin(RoutingRoot)
    val allRoutes = allRoutes(root)
    val allRoutesWithMethod = allRoutes.filter { it.selector is HttpMethodRouteSelector }
    allRoutesWithMethod.forEach {
        println("route: $it")
    }
}

fun allRoutes(root: RoutingNode): List<RoutingNode> {
    return listOf(root) + root.children.flatMap { allRoutes(it) }
}
