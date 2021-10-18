package com.ramitsuri.routes

import com.ramitsuri.models.Error
import com.ramitsuri.models.ErrorCode
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.TaskAssignmentDto
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.time.Instant

class TaskAssignmentRoutes(
    private val baseRoute: String,
    private val taskAssignmentsRepository: TaskAssignmentsRepository
): Routes() {

    override fun register(application: Application) {
        val invalidIdParamError = getInvalidIdParamError()
        application.routing {
            route(baseRoute) {

                // Get all
                get {
                    call.respond(taskAssignmentsRepository.get())
                }

                // Get by ID
                get("{id}") {
                    val id = call.parameters["id"] ?: return@get call.respond(
                        invalidIdParamError.first,
                        invalidIdParamError.second
                    )
                    val task = taskAssignmentsRepository.get(id)
                    if (task != null) {
                        call.respond(task)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            Error(
                                ErrorCode.NOT_FOUND,
                                "Task Assignment not found"
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
                    val result = taskAssignmentsRepository.delete(id)
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
                    val assignmentDto = call.receive<TaskAssignmentDto>()
                    val progressStatus = ProgressStatus.fromKey(assignmentDto.progressStatus)
                    if (progressStatus == ProgressStatus.UNKNOWN) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            Error(
                                ErrorCode.INVALID_REQUEST,
                                "Invalid progress status"
                            )
                        )
                    }
                    val result = taskAssignmentsRepository.edit(id, progressStatus, Instant.now())
                    if (result == 1) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
}