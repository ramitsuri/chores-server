package com.ramitsuri.di

import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.InstantConverter
import com.ramitsuri.data.UuidConverter
import com.ramitsuri.models.RepeatSchedulerConfig
import com.ramitsuri.models.SchedulerRepeatType
import com.ramitsuri.repeater.RepeatScheduler
import com.ramitsuri.repeater.TaskRepeater
import com.ramitsuri.repository.*
import com.ramitsuri.routes.*
import com.ramitsuri.utils.DummyRepository
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import java.time.ZoneId

class AppContainer {
    private val uuidConverter = UuidConverter()
    private val instantConverter = InstantConverter()
    private val housesRepository = HousesRepositoryImpl(uuidConverter, instantConverter)
    private val membersRepository = MembersRepositoryImpl(instantConverter, uuidConverter)
    private val tasksRepository = TasksRepositoryImpl(housesRepository, instantConverter, uuidConverter)
    private val taskAssignmentsRepository =
        TaskAssignmentsRepositoryImpl(tasksRepository, membersRepository, instantConverter, uuidConverter)
    private val memberAssignmentsRepository =
        MemberAssignmentsRepositoryImpl(membersRepository, housesRepository, uuidConverter)
    private val dummyRepository = DummyRepository(
        membersRepository,
        housesRepository,
        tasksRepository,
        memberAssignmentsRepository,
        taskAssignmentsRepository
    )

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