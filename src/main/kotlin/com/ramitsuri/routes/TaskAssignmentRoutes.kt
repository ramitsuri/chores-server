package com.ramitsuri.routes

import com.ramitsuri.models.AccessResult
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.models.TaskAssignmentDto
import com.ramitsuri.plugins.getMemberId
import com.ramitsuri.repository.access.TaskAssignmentAccessController
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import io.ktor.server.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

class TaskAssignmentRoutes(
    private val taskAssignmentsAccessController: TaskAssignmentAccessController
) : Routes() {
    override val path = "/task-assignments"

    override val routes: Route.() -> Unit = {
        // Get for filter
        get("filter") {
            val requesterMemberId = this.context.getMemberId() ?: return@get call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )
            val memberId = call.request.queryParameters["member"]
            val notMemberId = call.request.queryParameters["notmember"]
            val progressStatus = try {
                ProgressStatus.fromKey(
                    call.request.queryParameters["progress"]?.toInt() ?: ProgressStatus.UNKNOWN.key
                )
            } catch (e: Exception) {
                ProgressStatus.UNKNOWN
            }
            val filter = TaskAssignmentFilter(
                memberId = memberId,
                notMemberId = notMemberId,
                progressStatus = progressStatus,
                onlyActiveAndPausedHouse = true // Defaulting to true for now
            )
            when (val accessResult = taskAssignmentsAccessController.get(requesterMemberId, filter)) {
                is AccessResult.Success -> {
                    call.respond(accessResult.data)
                }
                is AccessResult.Failure -> {
                    call.respond(HttpStatusCode.Unauthorized, "Not sufficient access")
                }
            }
        }

        // Edit
        put {
            val requesterMemberId = this.context.getMemberId() ?: return@put call.respond(
                invalidTokenError.first,
                invalidTokenError.second
            )
            val assignmentDtos = call.receive<List<TaskAssignmentDto>>()
                .filter { ProgressStatus.fromKey(it.progressStatus) != ProgressStatus.UNKNOWN }
            when (val accessResult = taskAssignmentsAccessController.edit(requesterMemberId, assignmentDtos)) {
                is AccessResult.Success -> {
                    call.respond(accessResult.data)
                }
                is AccessResult.Failure -> {
                    call.respond(HttpStatusCode.Unauthorized, "Not sufficient access")
                }
            }
        }
    }
}