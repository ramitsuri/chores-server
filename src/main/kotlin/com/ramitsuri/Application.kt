package com.ramitsuri

import com.ramitsuri.di.AppContainer
import com.ramitsuri.environment.getDbDriver
import com.ramitsuri.environment.getDbPassword
import com.ramitsuri.environment.getDbUrl
import com.ramitsuri.environment.getDbUsername
import com.ramitsuri.plugins.configureSerialization
import io.ktor.server.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun main() {
    val appContainer = AppContainer()
    appContainer.getDatabase().init(getDbUrl(), getDbDriver(), getDbUsername(), getDbPassword())
    CoroutineScope(Dispatchers.Default).launch {
        appContainer.getTaskScheduler().schedule()
    }
    /*CoroutineScope(Dispatchers.Default).launch {
        appContainer.getTestTaskScheduler().schedule()
    }*/
    embeddedServer(appContainer.getApplicationEngine(), port = 8081, configure = {
        connectionGroupSize = 2
        workerGroupSize = 5
        callGroupSize = 10
    }) {
        // configureSecurity()
        /*install(CORS) {
            anyHost()
        }*/
        for (routes in appContainer.getRoutes()) {
            routes.register(this)
        }
        configureSerialization()
    }.start(wait = true)
}
