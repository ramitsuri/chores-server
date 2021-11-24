package com.ramitsuri.di

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.FirestoreOptions
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.InstantConverter
import com.ramitsuri.data.Migration
import com.ramitsuri.data.UuidConverter
import com.ramitsuri.events.EventService
import com.ramitsuri.events.GuavaEventService
import com.ramitsuri.models.RepeatSchedulerConfig
import com.ramitsuri.models.SchedulerRepeatType
import com.ramitsuri.repeater.RepeatScheduler
import com.ramitsuri.repeater.TaskRepeater
import com.ramitsuri.repository.local.*
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

    private val housesRepository = LocalHousesRepository(uuidConverter, instantConverter)
    private val remoteHousesRepository = RemoteHousesRepository("Houses", firebaseDb, uuidConverter, instantConverter)

    private val membersRepository = LocalMembersRepository(instantConverter, uuidConverter)
    private val remoteMembersRepository =
        RemoteMembersRepository("Members", firebaseDb, instantConverter, uuidConverter)

    private val tasksRepository = LocalTasksRepository(housesRepository, instantConverter, uuidConverter)
    private val remoteTasksRepository =
        RemoteTasksRepository("Tasks", firebaseDb, remoteHousesRepository, instantConverter, uuidConverter)

    private val taskAssignmentsRepository =
        LocalTaskAssignmentsRepository(
            tasksRepository,
            membersRepository,
            instantConverter,
            uuidConverter
        )
    private val remoteTaskAssignmentsRepository =
        RemoteTaskAssignmentsRepository(
            "TaskAssignments",
            firebaseDb,
            remoteTasksRepository,
            remoteMembersRepository,
            instantConverter,
            uuidConverter
        )

    private val memberAssignmentsRepository =
        LocalMemberAssignmentsRepository(
            membersRepository,
            housesRepository,
            uuidConverter
        )
    private val remoteMemberAssignmentsRepository =
        RemoteMemberAssignmentsRepository(
            "MemberAssignments",
            firebaseDb,
            remoteMembersRepository,
            remoteHousesRepository,
            uuidConverter
        )

    private val dummyRepository = DummyRepository(
        membersRepository,
        housesRepository,
        tasksRepository,
        memberAssignmentsRepository,
        taskAssignmentsRepository
    )

    // For Test API
    private val testTasksRepository =
        LocalTasksRepository(housesRepository, instantConverter, uuidConverter)
    private val testTaskAssignmentsRepository =
        LocalTaskAssignmentsRepository(
            testTasksRepository,
            membersRepository,
            instantConverter,
            uuidConverter
        )

    private val eventService: EventService = GuavaEventService()
    fun getEventService() = eventService

    fun getRoutes(): List<Routes> {
        return listOf(
            HouseRoutes(housesRepository),
            MemberRoutes(membersRepository),
            TaskRoutes("/tasks", tasksRepository, instantConverter),
            TaskAssignmentRoutes("/task-assignments", taskAssignmentsRepository),
            MemberAssignmentRoutes(memberAssignmentsRepository),
            DummyRoutes(dummyRepository),
            //TaskRoutes("/test/tasks", testTasksRepository, instantConverter),
            //TaskAssignmentRoutes("/test/task-assignments", testTaskAssignmentsRepository)
            MigrationRoutes(
                Migration(
                    membersRepository,
                    remoteMembersRepository,
                    housesRepository,
                    remoteHousesRepository,
                    memberAssignmentsRepository,
                    remoteMemberAssignmentsRepository,
                    tasksRepository,
                    remoteTasksRepository,
                    taskAssignmentsRepository,
                    remoteTaskAssignmentsRepository
                )
            )
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
        return RepeatScheduler(config, repeater)
    }

    fun getTestTaskScheduler(): RepeatScheduler {
        val repeater =
            TaskRepeater(
                eventService,
                testTasksRepository,
                membersRepository,
                memberAssignmentsRepository,
                testTaskAssignmentsRepository,
                Dispatchers.Default
            )
        val config = RepeatSchedulerConfig(
            repeatType = SchedulerRepeatType.HOUR,
            zoneId = ZoneId.of("UTC")
        )
        return RepeatScheduler(config, repeater)
    }

    @OptIn(EngineAPI::class)
    fun getApplicationEngine(): Netty {
        return Netty
    }
}