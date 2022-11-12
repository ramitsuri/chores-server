package com.ramitsuri.routes

import com.ramitsuri.data.LocalDateTimeConverter
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.Error
import com.ramitsuri.models.ErrorCode
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.TaskDto
import com.ramitsuri.repository.interfaces.TasksRepository
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import java.time.Instant

class TaskRoutes(
    private val tasksRepository: TasksRepository,
    private val localDateTimeConverter: LocalDateTimeConverter
) : Routes(), Loggable {
    override val log = logger()

    override val path = "/tasks"

    override val routes: Route.() -> Unit = {
        // Get all
        get {
            call.respond(tasksRepository.get())
        }

        // Get by ID
        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                invalidIdParamError.first,
                invalidIdParamError.second
            )
            val task = tasksRepository.get(id)
            if (task != null) {
                call.respond(task)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    Error(
                        ErrorCode.NOT_FOUND,
                        "Task not found"
                    )
                )
            }
        }

        // Add
        post {
            val taskDto = call.receive<TaskDto>()
            val name = taskDto.name
            val description = taskDto.description
            val dueDateTime =
                if (taskDto.dueDateTime != null) localDateTimeConverter.toMain(taskDto.dueDateTime) else null
            val repeatValue = taskDto.repeatValue ?: 0
            val repeatUnit = RepeatUnit.fromKey(taskDto.repeatUnit ?: RepeatUnit.NONE.key)
            val houseId = taskDto.houseId
            val memberId = taskDto.memberId
            val rotateMember = taskDto.rotateMember ?: false
            if (name != null && description != null && houseId != null && dueDateTime != null && memberId != null) {
                val createdTask =
                    tasksRepository.add(
                        name,
                        description,
                        dueDateTime,
                        repeatValue,
                        repeatUnit,
                        houseId,
                        memberId,
                        rotateMember,
                        Instant.now(),
                        status = ActiveStatus.ACTIVE // Adding new tasks with Active status as default for now
                    )
                if (createdTask != null) {
                    call.respond(
                        HttpStatusCode.Created,
                        createdTask
                    )
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        Error(
                            ErrorCode.INTERNAL_ERROR,
                            "Error creating task"
                        )
                    )
                }
            } else {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    Error(
                        ErrorCode.INVALID_REQUEST,
                        "name, description, dueDateTime, memberId and houseId are required"
                    )
                )
            }
        }

        // Delete by Id
        delete("{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(
                invalidIdParamError.first,
                invalidIdParamError.second
            )
            val result = tasksRepository.delete(id)
            if (result == 1) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Edit
        put("{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(
                invalidIdParamError.first,
                invalidIdParamError.second
            )
            val existingTask = tasksRepository.get(id) ?: return@put call.respond(HttpStatusCode.NotFound)
            val taskDto = call.receive<TaskDto>()
            val name = taskDto.name ?: existingTask.name
            val description = taskDto.description ?: existingTask.description
            val dueDateTime =
                if (taskDto.dueDateTime != null) localDateTimeConverter.toMain(taskDto.dueDateTime) else existingTask.dueDateTime
            val repeatValue: Int
            val repeatUnit: RepeatUnit
            if (taskDto.repeatUnit == null || taskDto.repeatValue == null) {
                repeatValue = existingTask.repeatValue
                repeatUnit = existingTask.repeatUnit
            } else {
                repeatValue = taskDto.repeatValue
                repeatUnit = RepeatUnit.fromKey(taskDto.repeatUnit)
            }
            val rotateMember = taskDto.rotateMember ?: existingTask.rotateMember
            val status = taskDto.status ?: existingTask.status
            val result =
                tasksRepository.edit(id, name, description, dueDateTime, repeatValue, repeatUnit, rotateMember, status)
            if (result == 1) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}