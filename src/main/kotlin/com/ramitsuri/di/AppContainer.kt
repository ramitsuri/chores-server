package com.ramitsuri.di

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.FirestoreOptions
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.InstantConverter
import com.ramitsuri.data.UuidConverter
import com.ramitsuri.events.EventService
import com.ramitsuri.events.GuavaEventService
import com.ramitsuri.models.RepeatSchedulerConfig
import com.ramitsuri.models.SchedulerRepeatType
import com.ramitsuri.repeater.RepeatScheduler
import com.ramitsuri.repeater.TaskRepeater
import com.ramitsuri.repository.remote.*
import com.ramitsuri.routes.*
import com.ramitsuri.utils.DummyRepository
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import java.time.ZoneId

class AppContainer {
    private val uuidConverter = UuidConverter()
    private val instantConverter = InstantConverter()

    private val firebaseOptions = FirestoreOptions.getDefaultInstance().toBuilder()
        .setProjectId("chores-326817")
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build()
    private val firebaseDb = firebaseOptions.service

    private val housesRepository = RemoteHousesRepository(firebaseDb, uuidConverter, instantConverter)
    private val membersRepository = RemoteMembersRepository(firebaseDb, instantConverter, uuidConverter)
    private val tasksRepository = RemoteTasksRepository(firebaseDb, housesRepository, instantConverter, uuidConverter)
    private val taskAssignmentsRepository =
        RemoteTaskAssignmentsRepository(firebaseDb, tasksRepository, membersRepository, instantConverter, uuidConverter)
    private val memberAssignmentsRepository =
        RemoteMemberAssignmentsRepository(firebaseDb, membersRepository, housesRepository, uuidConverter)
    private val dummyRepository = DummyRepository(
        membersRepository,
        housesRepository,
        tasksRepository,
        memberAssignmentsRepository,
        taskAssignmentsRepository
    )

    private val eventService: EventService = GuavaEventService()
    fun getEventService() = eventService

    fun getRoutes(): List<Routes> {
        return listOf(
            HouseRoutes(housesRepository),
            MemberRoutes(membersRepository),
            TaskRoutes(tasksRepository, instantConverter),
            TaskAssignmentRoutes(taskAssignmentsRepository),
            MemberAssignmentRoutes(memberAssignmentsRepository),
            DummyRoutes(dummyRepository)
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
            repeatType = SchedulerRepeatType.MINUTE,
            zoneId = ZoneId.of("UTC")
        )
        return RepeatScheduler(config, repeater)
    }

    @OptIn(EngineAPI::class)
    fun getApplicationEngine(): Netty {
        return Netty
    }
}