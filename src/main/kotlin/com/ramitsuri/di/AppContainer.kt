package com.ramitsuri.di

import com.ramitsuri.Constants
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.InstantConverter
import com.ramitsuri.data.UuidConverter
import com.ramitsuri.environment.EnvironmentRepository
import com.ramitsuri.events.EventService
import com.ramitsuri.events.GuavaEventService
import com.ramitsuri.models.RepeatSchedulerConfig
import com.ramitsuri.models.SchedulerRepeatType
import com.ramitsuri.plugins.JwtService
import com.ramitsuri.repeater.RepeatScheduler
import com.ramitsuri.repeater.TaskRepeater
import com.ramitsuri.repository.access.SyncAccessController
import com.ramitsuri.repository.access.TaskAssignmentAccessController
import com.ramitsuri.repository.local.*
import com.ramitsuri.routes.*
import com.ramitsuri.utils.DummyRepository
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import java.time.ZoneId

class AppContainer {
    val environment = EnvironmentRepository()
    private val uuidConverter = UuidConverter()
    private val instantConverter = InstantConverter()

    private val housesRepository = LocalHousesRepository(uuidConverter, instantConverter)
    private val membersRepository = LocalMembersRepository(instantConverter, uuidConverter)
    private val tasksRepository = LocalTasksRepository(housesRepository, instantConverter, uuidConverter)
    private val taskAssignmentsRepository =
        LocalTaskAssignmentsRepository(
            tasksRepository,
            membersRepository,
            instantConverter,
            uuidConverter
        )
    private val memberAssignmentsRepository =
        LocalMemberAssignmentsRepository(
            membersRepository,
            housesRepository,
            uuidConverter
        )
    private val taskAssignmentsAccessController =
        TaskAssignmentAccessController(memberAssignmentsRepository, taskAssignmentsRepository, membersRepository)
    private val syncRepository = LocalSyncRepository(memberAssignmentsRepository, housesRepository)
    private val syncAccessController = SyncAccessController(syncRepository, membersRepository)
    private val dummyRepository = DummyRepository(
        membersRepository,
        housesRepository,
        tasksRepository,
        memberAssignmentsRepository,
        taskAssignmentsRepository
    )

    private val eventService: EventService = GuavaEventService()

    private val jwtService = JwtService(
        environment.getJwtIssuer(),
        membersRepository,
        Constants.TOKEN_EXPIRATION,
        environment.getJwtSecret()
    )

    fun getJwtService() = jwtService

    fun getRoutes(): List<Routes> {
        return listOf(
            //HouseRoutes(housesRepository),
            //MemberRoutes(membersRepository),
            //TaskRoutes(tasksRepository, instantConverter),
            TaskAssignmentRoutes(taskAssignmentsAccessController),
            //MemberAssignmentRoutes(memberAssignmentsRepository),
            LoginRoutes(jwtService, membersRepository),
            SyncRoutes(syncAccessController)
            //DummyRoutes(dummyRepository),
        )
    }

    fun getDatabase(): DatabaseFactory {
        return DatabaseFactory
    }

    fun getTaskScheduler(): RepeatScheduler {
        val repeater =
            TaskRepeater(
                eventService,
                tasksRepository,
                membersRepository,
                memberAssignmentsRepository,
                taskAssignmentsRepository,
                Dispatchers.Default
            )
        val config = RepeatSchedulerConfig(
            repeatType = SchedulerRepeatType.HOUR,
            zoneId = ZoneId.of("UTC")
        )
        return RepeatScheduler(config, repeater, LocalRunTimeLogsRepository(instantConverter))
    }

    @OptIn(EngineAPI::class)
    fun getApplicationEngine(): Netty {
        return Netty
    }
}