package com.ramitsuri.di

import com.ramitsuri.Constants
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.InstantConverter
import com.ramitsuri.data.LocalDateTimeConverter
import com.ramitsuri.data.UuidConverter
import com.ramitsuri.environment.EnvironmentRepository
import com.ramitsuri.events.EventService
import com.ramitsuri.events.SystemEventService
import com.ramitsuri.plugins.JwtService
import com.ramitsuri.pushmessage.FirebasePushMessageDispatcher
import com.ramitsuri.pushmessage.PushMessageDispatcher
import com.ramitsuri.pushmessage.PushMessagePayloadGenerator
import com.ramitsuri.pushmessage.PushMessageService
import com.ramitsuri.repeater.RepeatScheduler
import com.ramitsuri.repeater.RepeatSchedulerConfig
import com.ramitsuri.repeater.SchedulerRepeatType
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.Clock
import java.time.ZoneId

class AppContainer {
    val environment = EnvironmentRepository()
    private val uuidConverter = UuidConverter()
    private val instantConverter = InstantConverter()
    private val localDateTimeConverter = LocalDateTimeConverter()

    private val eventService: EventService = SystemEventService()

    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val housesRepository = LocalHousesRepository(uuidConverter, instantConverter)
    private val membersRepository = LocalMembersRepository(instantConverter, uuidConverter)
    private val tasksRepository =
        LocalTasksRepository(housesRepository, instantConverter, localDateTimeConverter, uuidConverter, eventService)
    private val taskAssignmentsRepository =
        LocalTaskAssignmentsRepository(
            tasksRepository,
            membersRepository,
            housesRepository,
            instantConverter,
            localDateTimeConverter,
            uuidConverter,
            eventService,
        )
    private val tasksTaskAssignmentsRepository =
        LocalTasksTaskAssignmentsRepository(localDateTimeConverter, uuidConverter, eventService)
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
    private val pushMessageTokenRepository = LocalPushMessageTokenRepository(uuidConverter, instantConverter)

    private val jwtService = JwtService(
        environment.getJwtIssuer(),
        membersRepository,
        Constants.TOKEN_EXPIRATION,
        environment.getJwtSecret()
    )

    val dummyDataProvider = DummyDataProvider()

    private val pushMessageDispatcher: PushMessageDispatcher = FirebasePushMessageDispatcher()
    private val pushMessagePayloadGenerator = PushMessagePayloadGenerator(
        tasksRepository = tasksRepository,
        taskAssignmentsRepository = taskAssignmentsRepository,
        memberAssignmentsRepository = memberAssignmentsRepository,
        pushMessageTokenRepository = pushMessageTokenRepository
    )

    init {
        PushMessageService(
            pushMessageDispatcher = pushMessageDispatcher,
            pushMessagePayloadGenerator = pushMessagePayloadGenerator,
            coroutineScope = coroutineScope,
            ioDispatcher = Dispatchers.IO,
            eventService = eventService
        )
    }

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
        )
    }

    fun getDatabase(): DatabaseFactory {
        return DatabaseFactory
    }

    fun getTaskScheduler(): RepeatScheduler {
        val repeater =
            TaskRepeater(
                tasksRepository,
                membersRepository,
                housesRepository,
                memberAssignmentsRepository,
                taskAssignmentsRepository,
                Dispatchers.IO
            )
        val config = RepeatSchedulerConfig(
            repeatType = SchedulerRepeatType.HOUR,
            zoneId = ZoneId.of("UTC")
        )
        return RepeatScheduler(
            config = config,
            taskRepeater = repeater,
            runTimeLogRepository = LocalRunTimeLogsRepository(instantConverter),
            clock = Clock.System,
            coroutineScope = coroutineScope,
            ioDispatcher = Dispatchers.IO,
            eventService = eventService
        )
    }

    fun getApplicationEngine(): Netty {
        return Netty
    }
}