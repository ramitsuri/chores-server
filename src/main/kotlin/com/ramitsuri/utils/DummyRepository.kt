package com.ramitsuri.utils

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.CreateType
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.repository.interfaces.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

class DummyRepository(
    private val membersRepository: MembersRepository,
    private val housesRepository: HousesRepository,
    private val tasksRepository: TasksRepository,
    private val memberAssignmentsRepository: MemberAssignmentsRepository,
    private val taskAssignmentsRepository: TaskAssignmentsRepository
) {
    fun add() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Members
                val member1 = membersRepository.add("Ramit", Instant.now())
                val member2 = membersRepository.add("Jess", Instant.now())
                // Houses
                val house1 = housesRepository.add("House", member1!!.id, Instant.now(), ActiveStatus.ACTIVE)

                // Member Assignments
                memberAssignmentsRepository.add(member1.id, house1!!.id)
                memberAssignmentsRepository.add(member2!!.id, house1.id)

                // Tasks
                val task1 =
                    tasksRepository.add(
                        "Toilet Bowl",
                        "Clean toilet bowls",
                        Instant.now().plusSeconds(3600),
                        2,
                        RepeatUnit.WEEK,
                        house1.id,
                        member1.id,
                        true,
                        Instant.now()
                    )
                val task2 =
                    tasksRepository.add(
                        "Kitchen Sink",
                        "Clean kitchen sink",
                        Instant.now().plusSeconds(7200),
                        3,
                        RepeatUnit.WEEK,
                        house1.id,
                        member1.id,
                        true,
                        Instant.now()
                    )
                val task3 =
                    tasksRepository.add(
                        "Kitchen Counter",
                        "Clean kitchen counter",
                        Instant.now().plusSeconds(7200),
                        1,
                        RepeatUnit.DAY,
                        house1.id,
                        member2.id,
                        false,
                        Instant.now()
                    )

                // Task Assignments
                taskAssignmentsRepository.add(
                    ProgressStatus.TODO,
                    Instant.now(),
                    task1!!.id,
                    member1.id,
                    task1.dueDateTime,
                    Instant.now(),
                    CreateType.MANUAL
                )
                taskAssignmentsRepository.add(
                    ProgressStatus.TODO,
                    Instant.now(),
                    task2!!.id,
                    member1.id,
                    task2.dueDateTime,
                    Instant.now(),
                    CreateType.MANUAL
                )
                taskAssignmentsRepository.add(
                    ProgressStatus.TODO,
                    Instant.now(),
                    task3!!.id,
                    member2.id,
                    task3.dueDateTime,
                    Instant.now(),
                    CreateType.MANUAL
                )
            } catch (e: Exception) {
                // Do nothing for now
            }
        }
    }
}