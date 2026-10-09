package com.ramitsuri.routes

import com.ramitsuri.data.LocalDateTimeConverter
import com.ramitsuri.extensions.Loggable
import com.ramitsuri.models.AccessResult
import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.Error
import com.ramitsuri.models.ErrorCode
import com.ramitsuri.models.RepeatUnit
import com.ramitsuri.models.TaskDto
import com.ramitsuri.plugins.getMemberId
import com.ramitsuri.repository.access.TaskAccessController
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import java.time.Instant

class TaskRoutes(
    private val taskAccessController: TaskAccessController,
    private val localDateTimeConverter: LocalDateTimeConverter
) : Routes(), Loggable {
    override val log = logger()

    override val path = "/tasks"

    override val routes: Route.() -> Unit = {
        // Get all
        get {
            val requesterMemberId = call.getMemberId() ?: return@get call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )
            when (val accessResult = taskAccessController.get(requesterMemberId)) {
                is AccessResult.Success -> {
                    call.respond(accessResult.data)
                }
                is AccessResult.Failure -> {
                    call.respond(HttpStatusCode.Unauthorized, "Not sufficient access")
                }
            }
        }

        // Get by ID
        get("{id}") {
            val requesterMemberId = call.getMemberId() ?: return@get call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )
            val id = call.parameters["id"] ?: return@get call.respond(
                invalidIdParamError.first,
                invalidIdParamError.second
            )
            when (val accessResult = taskAccessController.get(requesterMemberId, id)) {
                is AccessResult.Success -> {
                    val task = accessResult.data
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
                is AccessResult.Failure -> {
                    call.respond(HttpStatusCode.Unauthorized, "Not sufficient access")
                }
            }
        }

        // Add
        post {
            val requesterMemberId = call.getMemberId() ?: return@post call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )
            val taskDto = call.receive<TaskDto>()
            val name = taskDto.name
            val description = taskDto.description
            val dueDateTime =
                if (taskDto.dueDateTime != null) localDateTimeConverter.toMain(taskDto.dueDateTime) else null
            val repeatValue = taskDto.repeatValue ?: 0
            val repeatUnit = RepeatUnit.fromKey(taskDto.repeatUnit ?: RepeatUnit.NONE.key)
            val repeatEndDateTime = if (taskDto.repeatEndDateTime != null) {
                localDateTimeConverter.toMain(taskDto.repeatEndDateTime)
            } else {
                null
            }
            val houseId = taskDto.houseId
            val memberId = taskDto.memberId
            val rotateMember = taskDto.rotateMember ?: false
            if (name != null && description != null && houseId != null && dueDateTime != null && memberId != null) {
                val accessResult = taskAccessController.add(
                    requesterMemberId,
                    name,
                    description,
                    dueDateTime,
                    repeatValue,
                    repeatUnit,
                    repeatEndDateTime,
                    houseId,
                    memberId,
                    rotateMember,
                    Instant.now(),
                    status = ActiveStatus.ACTIVE // Adding new tasks with Active status as default for now
                )
                when (accessResult) {
                    is AccessResult.Success -> {
                        val createdTask = accessResult.data
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
                    }
                    is AccessResult.Failure -> {
                        call.respond(HttpStatusCode.Unauthorized, "Not sufficient access")
                    }
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
            val requesterMemberId = call.getMemberId() ?: return@delete call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )
            val taskId = call.parameters["id"] ?: return@delete call.respond(
                invalidIdParamError.first,
                invalidIdParamError.second
            )
            when (val accessResult = taskAccessController.delete(requesterMemberId, taskId)) {
                is AccessResult.Success -> {
                    if (accessResult.data) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                is AccessResult.Failure -> {
                    call.respond(HttpStatusCode.Unauthorized, "Not sufficient access")
                }
            }
        }

        // Edit
        put("{id}") {
            val requesterMemberId = call.getMemberId() ?: return@put call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )
            val taskId = call.parameters["id"] ?: return@put call.respond(
                invalidIdParamError.first,
                invalidIdParamError.second
            )
            val existingTask = when (val accessResult = taskAccessController.get(requesterMemberId, taskId)) {
                is AccessResult.Success -> {
                    accessResult.data ?: return@put call.respond(HttpStatusCode.NotFound)
                }
                is AccessResult.Failure -> {
                    return@put call.respond(HttpStatusCode.Unauthorized, "Not sufficient access")
                }
            }
            val taskDto = call.receive<TaskDto>()

            var shouldDeleteTaskAssignments = false

            val name = taskDto.name ?: existingTask.name

            val description = taskDto.description ?: existingTask.description

            val dueDateTime = if (taskDto.dueDateTime != null) {
                shouldDeleteTaskAssignments = true
                localDateTimeConverter.toMain(taskDto.dueDateTime)
            } else {
                existingTask.dueDateTime
            }
            val repeatUnit = if (taskDto.repeatUnit != null) {
                shouldDeleteTaskAssignments = true
                RepeatUnit.fromKey(taskDto.repeatUnit)
            } else {
                existingTask.repeatUnit
            }
            val repeatValue = if (taskDto.repeatValue != null) {
                shouldDeleteTaskAssignments = true
                taskDto.repeatValue
            } else {
                existingTask.repeatValue
            }
            val repeatEndDateTime = if (taskDto.repeatEndDateTime != null) {
                shouldDeleteTaskAssignments = true
                localDateTimeConverter.toMain(taskDto.repeatEndDateTime)
            } else {
                existingTask.repeatEndDateTime
            }

            val rotateMember = if (taskDto.rotateMember != null) {
                shouldDeleteTaskAssignments = true
                taskDto.rotateMember
            } else {
                existingTask.rotateMember
            }

            val status = if (taskDto.status != null) {
                shouldDeleteTaskAssignments = true
                ActiveStatus.fromKey(taskDto.status)
            } else {
                existingTask.status
            }

            val accessResult = taskAccessController.edit(
                requesterMemberId,
                taskId,
                name,
                description,
                dueDateTime,
                repeatValue,
                repeatUnit,
                repeatEndDateTime,
                rotateMember,
                status,
                shouldDeleteTaskAssignments
            )
            when (accessResult) {
                is AccessResult.Success -> {
                    if (accessResult.data) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
                is AccessResult.Failure -> {
                    call.respond(HttpStatusCode.Unauthorized, "Not sufficient access")
                }
            }
        }
    }
}
