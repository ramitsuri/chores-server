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

class TestAppContainer {
    val uuidConverter = UuidConverter()
    val instantConverter = InstantConverter()
    val localDateTimeConverter = LocalDateTimeConverter()

    private val eventService = SystemEventService()

    val housesRepository = LocalHousesRepository(uuidConverter, instantConverter)
    val membersRepository = LocalMembersRepository(instantConverter, uuidConverter)
    val tasksRepository =
        LocalTasksRepository(housesRepository, instantConverter, localDateTimeConverter, uuidConverter, eventService)
    val taskAssignmentsRepository =
        LocalTaskAssignmentsRepository(
            tasksRepository,
            membersRepository,
            housesRepository,
            instantConverter,
            localDateTimeConverter,
            uuidConverter,
            eventService
        )
    val tasksTaskAssignmentsRepository =
        LocalTasksTaskAssignmentsRepository(localDateTimeConverter, uuidConverter, eventService)
    val memberAssignmentsRepository =
        LocalMemberAssignmentsRepository(
            membersRepository,
            housesRepository,
            uuidConverter
        )
    val pushMessageTokenRepository = LocalPushMessageTokenRepository(uuidConverter, instantConverter)
}