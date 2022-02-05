package com.ramitsuri.routes

import com.ramitsuri.models.Error
import com.ramitsuri.models.ErrorCode
import com.ramitsuri.models.MemberDto
import com.ramitsuri.repository.interfaces.MembersRepository
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.time.Instant

class MemberRoutes(private val membersRepository: MembersRepository) : Routes() {

    override val path = "/members"

    override val routes: Route.() -> Unit = {
        // Get all
        get {
            call.respond(membersRepository.get())
        }

        // Get by ID
        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                invalidIdParamError.first,
                invalidIdParamError.second
            )
            val member = membersRepository.get(id)
            if (member != null) {
                call.respond(member)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    Error(
                        ErrorCode.NOT_FOUND,
                        "Member not found"
                    )
                )
            }
        }

        // Add
        post {
            val memberDto = call.receive<MemberDto>()
            val name = memberDto.name
            if (name != null) {
                val createdMember = membersRepository.add(memberDto.name, Instant.now())
                if (createdMember != null) {
                    call.respond(
                        HttpStatusCode.Created,
                        createdMember
                    )
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        Error(
                            ErrorCode.INTERNAL_ERROR,
                            "Error creating member"
                        )
                    )
                }
            } else {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    Error(
                        ErrorCode.INVALID_REQUEST,
                        "name is required"
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
            val result = membersRepository.delete(id)
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
            val memberDto = call.receive<MemberDto>()
            if (memberDto.name != null) {
                val result = membersRepository.edit(id, memberDto.name)
                if (result == 1) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } else {
                call.respond(
                    HttpStatusCode.BadRequest,
                    Error(
                        ErrorCode.INVALID_REQUEST,
                        "name is required"
                    )
                )
            }
        }
    }
}