package com.ramitsuri.routes

import com.ramitsuri.Constants
import com.ramitsuri.models.AccessResult
import com.ramitsuri.models.Member
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.plugins.JwtService
import com.ramitsuri.plugins.configureSecurity
import com.ramitsuri.plugins.configureSerialization
import com.ramitsuri.repository.access.TaskAssignmentAccessController
import com.ramitsuri.repository.interfaces.TaskAssignmentFilter
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

class TaskAssignmentRoutesTest {

    private val accessController: TaskAssignmentAccessController = mockk()
    private val jwtService = JwtService("issuer", mockk(relaxed = true), Duration.ofDays(1), "secret")

    @Test
    fun filter_shouldParseCommaSeparatedProgressStatuses() = testApplication {
        val memberId = "member1"
        val member = Member(id = memberId, name = "Test", createdDate = Instant.now())
        val token = jwtService.generateToken(member)

        coEvery { accessController.get(memberId, any()) } returns AccessResult.Success(emptyList())

        application {
            configureSerialization()
            configureSecurity(
                jwtService,
                Constants.JWT_REALM,
                Constants.JWT_AUTH_CONFIG_BASE
            )
            TaskAssignmentRoutes(accessController).register(this)
        }

        val response = client.get("/task-assignments/filter?progress=1,2,3") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        println("RESPONSE STATUS: ${response.status}")
        assertEquals(HttpStatusCode.OK, response.status)

        val expectedFilter = TaskAssignmentFilter(
            memberId = null,
            notMemberId = null,
            progressStatuses = listOf(
                ProgressStatus.TODO,
                ProgressStatus.IN_PROGRESS,
                ProgressStatus.DONE
            ),
            onlyActiveAndPausedHouse = true,
            dueDateWithinDays = null
        )
        coVerify { accessController.get(memberId, expectedFilter) }
    }
}
