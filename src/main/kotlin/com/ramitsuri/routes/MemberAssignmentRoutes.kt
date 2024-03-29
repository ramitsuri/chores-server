package com.ramitsuri.routes

import com.ramitsuri.models.Error
import com.ramitsuri.models.ErrorCode
import com.ramitsuri.models.MemberAssignmentDto
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class MemberAssignmentRoutes(private val memberAssignmentsRepository: MemberAssignmentsRepository) : Routes() {

    override val path = "/member-assignments"
    override val routes: Route.() -> Unit = {
        // Get all
        get {
            call.respond(memberAssignmentsRepository.get())
        }

        // Get by ID
        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                invalidIdParamError.first,
                invalidIdParamError.second
            )
            val memberAssignment = memberAssignmentsRepository.get(id)
            if (memberAssignment != null) {
                call.respond(memberAssignment)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    Error(
                        ErrorCode.NOT_FOUND,
                        "Member assignment not found"
                    )
                )
            }
        }

        // Get by filter
        get("filter") {
            val houseId = call.request.queryParameters["houseId"]
            val assignments = if (houseId.isNullOrEmpty()) {
                memberAssignmentsRepository.get()
            } else {
                memberAssignmentsRepository.getForHouse(houseId)
            }
            call.respond(assignments)
        }

        // Add
        post {
            val memberAssignmentDto = call.receive<MemberAssignmentDto>()
            val memberId = memberAssignmentDto.memberId
            val houseId = memberAssignmentDto.houseId
            if (memberId != null && houseId != null) {
                val createdAssignment = memberAssignmentsRepository.add(memberId, houseId)
                if (createdAssignment != null) {
                    call.respond(
                        HttpStatusCode.Created,
                        createdAssignment
                    )
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        Error(
                            ErrorCode.INTERNAL_ERROR,
                            "Error creating member assignment"
                        )
                    )
                }
            } else {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    Error(
                        ErrorCode.INVALID_REQUEST,
                        "memberId and houseId are required"
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
            val result = memberAssignmentsRepository.delete(id)
            if (result == 1) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
