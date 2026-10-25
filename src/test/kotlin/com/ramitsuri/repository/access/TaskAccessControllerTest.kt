package com.ramitsuri.repository.access

import com.ramitsuri.data.LocalDateTimeConverter
import com.ramitsuri.models.Access
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.AddEditTaskDto
import com.ramitsuri.models.AddEditTaskResultDto
import com.ramitsuri.models.Member
import com.ramitsuri.models.MemberAssignment
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.Task
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import com.ramitsuri.repository.interfaces.MembersRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import com.ramitsuri.repository.interfaces.TasksTaskAssignmentsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.assertEquals

class TaskAccessControllerTest {

    private val memberAssignmentsRepository: MemberAssignmentsRepository = mockk()
    private val tasksRepository: TasksRepository = mockk()
    private val tasksTaskAssignmentsRepository: TasksTaskAssignmentsRepository = mockk()
    private val localDateTimeConverter = LocalDateTimeConverter()
    private val membersRepository: MembersRepository = mockk()

    private lateinit var controller: TaskAccessController

    private val memberId = "member1"
    private val houseId = "house1"

    @Before
    fun setUp() {
        controller = TaskAccessController(
            memberAssignmentsRepository,
            tasksRepository,
            tasksTaskAssignmentsRepository,
            localDateTimeConverter,
            membersRepository
        )

        val member = Member(id = memberId, name = "Member 1", createdDate = Instant.now(), access = Access.READ_HOUSE_WRITE_HOUSE)
        coEvery { membersRepository.get(memberId) } returns member
        coEvery { membersRepository.getAccess(memberId) } returns Access.READ_HOUSE_WRITE_HOUSE
        val memberAssignment = MemberAssignment(id = "assign1", member = member, houseId = houseId)
        coEvery { memberAssignmentsRepository.getForMember(memberId) } returns listOf(memberAssignment)
    }

    @Test
    fun addOrEdit_shouldReturnSuccessfulClientRequestIdsForAddAndEdit() = runBlocking {
        val addDto = AddEditTaskDto.Add(
            name = "New Task",
            description = "Desc",
            dueDateTime = "2025-01-01T10:00:00",
            houseId = houseId,
            memberId = memberId,
            clientRequestId = "req_add"
        )

        val createdTask = Task(
            id = "task1",
            name = "New Task",
            description = "Desc",
            dueDateTime = LocalDateTime.now(),
            repeatValue = 0,
            repeatUnit = RepeatUnit.NONE,
            repeatEndDateTime = null,
            houseId = houseId,
            memberId = memberId,
            rotateMember = false,
            createdDate = Instant.now(),
            status = ActiveStatus.ACTIVE
        )

        val existingTask = Task(
            id = "task2",
            name = "Existing Task",
            description = "Old Desc",
            dueDateTime = LocalDateTime.now(),
            repeatValue = 0,
            repeatUnit = RepeatUnit.NONE,
            repeatEndDateTime = null,
            houseId = houseId,
            memberId = memberId,
            rotateMember = false,
            createdDate = Instant.now(),
            status = ActiveStatus.ACTIVE
        )

        coEvery {
            tasksRepository.add(
                name = "New Task",
                description = "Desc",
                dueDate = any(),
                repeatValue = 0,
                repeatUnit = RepeatUnit.NONE,
                repeatEndDate = null,
                houseId = houseId,
                memberId = memberId,
                rotateMember = false,
                createdDate = any(),
                status = ActiveStatus.ACTIVE
            )
        } returns createdTask

        coEvery { tasksRepository.get(any<String>()) } returns existingTask

        val editDto = AddEditTaskDto.Edit(
            taskId = "task2",
            name = "Updated Task Name",
            clientRequestId = "req_edit",
            dueDateTime = "2025-01-02T10:00:00"
        )

        coEvery {
            tasksTaskAssignmentsRepository.edit(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyNullable(),
                any(),
                any()
            )
        } returns true

        val results = controller.addOrEdit(memberId, listOf(addDto, editDto))

        assertEquals(
            AddEditTaskResultDto(
                successfulRequestIds = listOf("req_add", "req_edit"),
                deleteTaskAssignmentsTaskIds = listOf("task2"),
                tasks = listOf(createdTask, existingTask)
            ),
            results
        )
    }

    @Test
    fun addOrEdit_shouldExcludeFailedEditRequest() = runBlocking {
        coEvery { tasksRepository.get("nonExistent") } returns null

        val editDto = AddEditTaskDto.Edit(
            taskId = "nonExistent",
            name = "Updated Task",
            clientRequestId = "req_edit_failed"
        )

        val results = controller.addOrEdit(memberId, listOf(editDto))

        assertEquals(
            AddEditTaskResultDto(
                successfulRequestIds = emptyList(),
                deleteTaskAssignmentsTaskIds = emptyList(),
                tasks = emptyList()
            ),
            results
        )
    }
}
