package com.ramitsuri.testutils

import com.ramitsuri.data.InstantConverter
import com.ramitsuri.data.LocalDateTimeConverter
import com.ramitsuri.data.UuidConverter
import com.ramitsuri.events.SystemEventService
import com.ramitsuri.repository.local.LocalHousesRepository
import com.ramitsuri.repository.local.LocalMemberAssignmentsRepository
import com.ramitsuri.repository.local.LocalMembersRepository
import com.ramitsuri.repository.local.LocalPushMessageTokenRepository
import com.ramitsuri.repository.local.LocalTaskAssignmentsRepository
import com.ramitsuri.repository.local.LocalTasksRepository
import com.ramitsuri.repository.local.LocalTasksTaskAssignmentsRepository
import kotlin.time.Clock

class TestAppContainer {
    val uuidConverter = UuidConverter()
    val instantConverter = InstantConverter()
    val localDateTimeConverter = LocalDateTimeConverter()

    val eventService = SystemEventService()

    val housesRepository = LocalHousesRepository(uuidConverter, instantConverter)
    val membersRepository = LocalMembersRepository(instantConverter, uuidConverter)
    val tasksRepository =
        LocalTasksRepository(housesRepository, instantConverter, localDateTimeConverter, uuidConverter, eventService)
    val memberAssignmentsRepository =
        LocalMemberAssignmentsRepository(
            membersRepository,
            housesRepository,
            uuidConverter
        )
    val taskAssignmentsRepository =
        LocalTaskAssignmentsRepository(
            tasksRepository,
            membersRepository,
            housesRepository,
            memberAssignmentsRepository,
            instantConverter,
            localDateTimeConverter,
            uuidConverter,
            eventService,
            clock = Clock.System,
        )
    val tasksTaskAssignmentsRepository =
        LocalTasksTaskAssignmentsRepository(
            localDateTimeConverter,
            instantConverter,
            uuidConverter,
            eventService,
            clock = Clock.System,
        )
    val pushMessageTokenRepository = LocalPushMessageTokenRepository(uuidConverter, instantConverter)
}
