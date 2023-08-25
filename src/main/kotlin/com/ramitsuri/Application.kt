package com.ramitsuri

import com.ramitsuri.di.AppContainer
import com.ramitsuri.plugins.configureSecurity
import com.ramitsuri.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    CoroutineScope(Dispatchers.Default).launch {
        appContainer.getTaskScheduler().schedule()
    }
    embeddedServer(appContainer.getApplicationEngine(), port = 8081, configure = {
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
    val root = plugin(Routing)
    val allRoutes = allRoutes(root)
    val allRoutesWithMethod = allRoutes.filter { it.selector is HttpMethodRouteSelector }
    allRoutesWithMethod.forEach {
        println("route: $it")
    }
}

fun allRoutes(root: Route): List<Route> {
    return listOf(root) + root.children.flatMap { allRoutes(it) }
}
