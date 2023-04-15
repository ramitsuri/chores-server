package com.ramitsuri.di

import com.ramitsuri.Constants
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.InstantConverter
import com.ramitsuri.data.LocalDateTimeConverter
import com.ramitsuri.data.UuidConverter
import com.ramitsuri.environment.EnvironmentRepository
import com.ramitsuri.events.EventService
import com.ramitsuri.events.GuavaEventService
import com.ramitsuri.models.RepeatSchedulerConfig
import com.ramitsuri.models.SchedulerRepeatType
import com.ramitsuri.plugins.JwtService
import com.ramitsuri.pushmessage.FirebasePushMessageService
import com.ramitsuri.pushmessage.PushMessageService
import com.ramitsuri.repeater.RepeatScheduler
import com.ramitsuri.repeater.TaskRepeater
import com.ramitsuri.repository.access.SyncAccessController
import com.ramitsuri.repository.access.TaskAssignmentAccessController
import com.ramitsuri.repository.local.LocalHousesRepository
import com.ramitsuri.repository.local.LocalMemberAssignmentsRepository
import com.ramitsuri.repository.local.LocalMembersRepository
import com.ramitsuri.repository.local.LocalPushMessageTokenRepository
import com.ramitsuri.repository.local.LocalRunTimeLogsRepository
import com.ramitsuri.repository.local.LocalSyncRepository
import com.ramitsuri.repository.local.LocalTaskAssignmentsRepository
import com.ramitsuri.repository.local.LocalTasksRepository
import com.ramitsuri.repository.local.LocalTasksTaskAssignmentsRepository
import com.ramitsuri.routes.HouseRoutes
import com.ramitsuri.routes.LoginRoutes
import com.ramitsuri.routes.MemberAssignmentRoutes
import com.ramitsuri.routes.MemberRoutes
import com.ramitsuri.routes.PushMessageTokenRoute
import com.ramitsuri.routes.Routes
import com.ramitsuri.routes.SyncRoutes
import com.ramitsuri.routes.TaskAssignmentRoutes
import com.ramitsuri.routes.TaskRoutes
import com.ramitsuri.utils.DummyDataProvider
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import java.time.ZoneId

class AppContainer {
    val environment = EnvironmentRepository()
    private val uuidConverter = UuidConverter()
    private val instantConverter = InstantConverter()
    private val localDateTimeConverter = LocalDateTimeConverter()

    private val housesRepository = LocalHousesRepository(uuidConverter, instantConverter)
    private val membersRepository = LocalMembersRepository(instantConverter, uuidConverter)
    private val tasksRepository =
        LocalTasksRepository(housesRepository, instantConverter, localDateTimeConverter, uuidConverter)
    private val taskAssignmentsRepository =
        LocalTaskAssignmentsRepository(
            tasksRepository,
            membersRepository,
            housesRepository,
            instantConverter,
            localDateTimeConverter,
            uuidConverter
        )
    private val tasksTaskAssignmentsRepository =
        LocalTasksTaskAssignmentsRepository(localDateTimeConverter, uuidConverter)
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
    private val pushMessageTokenRepository = LocalPushMessageTokenRepository(uuidConverter)

    private val eventService: EventService = GuavaEventService()

    private val jwtService = JwtService(
        environment.getJwtIssuer(),
        membersRepository,
        Constants.TOKEN_EXPIRATION,
        environment.getJwtSecret()
    )

    val dummyDataProvider = DummyDataProvider()

    //val pushMessagingService: PushMessageService = FirebasePushMessageService()

    fun getJwtService() = jwtService

    fun getRoutes(): List<Routes> {
        return listOf(
            HouseRoutes(housesRepository),
            MemberRoutes(membersRepository),
            TaskRoutes(tasksRepository, tasksTaskAssignmentsRepository, localDateTimeConverter),
            TaskAssignmentRoutes(taskAssignmentsAccessController),
            MemberAssignmentRoutes(memberAssignmentsRepository),
            LoginRoutes(jwtService, membersRepository),
            SyncRoutes(syncAccessController),
            PushMessageTokenRoute(pushMessageTokenRepository)
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
                housesRepository,
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

    fun getApplicationEngine(): Netty {
        return Netty
    }
}