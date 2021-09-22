package com.ramitsuri.repeater

import com.ramitsuri.testutils.*
import java.time.ZoneId

open class BaseRepeaterTest {
    protected val zoneId: ZoneId = ZoneId.of("UTC")

    protected val housesRepository = TestHousesRepository()
    protected val membersRepository = TestMembersRepository()
    protected val memberAssignmentsRepository =
        TestMemberAssignmentsRepository(membersRepository, housesRepository)
    protected val tasksRepository = TestTasksRepository(housesRepository)
    protected val taskAssignmentsRepository = TestTaskAssignmentsRepository(tasksRepository, membersRepository)

    lateinit var taskRepeater: TaskRepeater
}