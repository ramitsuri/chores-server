package com.ramitsuri.routes

import com.ramitsuri.models.ActiveStatus
import com.ramitsuri.models.Error
import com.ramitsuri.models.ErrorCode
import com.ramitsuri.models.HouseDto
import com.ramitsuri.repository.interfaces.HousesRepository
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant

class HouseRoutes(private val housesRepository: HousesRepository) : Routes() {

    override val path = "/houses"

    override val routes: Route.() -> Unit = {
        // Get all
        get {
            call.respond(housesRepository.get())
        }

        // Get by ID
        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                invalidIdParamError.first,
                invalidIdParamError.second
            )
            val house = housesRepository.get(id)
            if (house != null) {
                call.respond(house)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    Error(
                        ErrorCode.NOT_FOUND,
                        "House not found"
                    )
                )
            }
        }

        // Add
        post {
            val houseDto = call.receive<HouseDto>()
            val name = houseDto.name
            val createdByMemberId = houseDto.createdByMemberId
            if (name != null && !createdByMemberId.isNullOrEmpty()) {
                val createdHouse = housesRepository.add(
                    houseDto.name,
                    createdByMemberId,
                    Instant.now(),
                    ActiveStatus.ACTIVE
                )
                if (createdHouse != null) {
                    call.respond(
                        HttpStatusCode.Created,
                        createdHouse
                    )
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        Error(
                            ErrorCode.INTERNAL_ERROR,
                            "Error creating house"
                        )
                    )
                }
            } else {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    Error(
                        ErrorCode.INVALID_REQUEST,
                        "name and createdByMemberId are required"
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
            val result = housesRepository.delete(id)
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
            val houseDto = call.receive<HouseDto>()
            if (houseDto.name != null) {
                val result = housesRepository.edit(id, houseDto.name)
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
