package com.ramitsuri.routes

import com.ramitsuri.Constants
import com.ramitsuri.data.LocalDateTimeConverter
import com.ramitsuri.models.AddEditTaskResultDto
import com.ramitsuri.models.Member
import com.ramitsuri.plugins.JwtService
import com.ramitsuri.plugins.configureSecurity
import com.ramitsuri.plugins.configureSerialization
import com.ramitsuri.repository.access.TaskAccessController
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskRoutesTest {

    private val accessController: TaskAccessController = mockk()
    private val localDateTimeConverter = LocalDateTimeConverter()
    private val jwtService = JwtService("issuer", mockk(relaxed = true), Duration.ofDays(1), "secret")

    @Test
    fun batch_shouldProcessAddAndEditRequests() = testApplication {
        val memberId = "member1"
        val member = Member(id = memberId, name = "Test", createdDate = Instant.now())
        val token = jwtService.generateToken(member)

        coEvery { accessController.addOrEdit(memberId, any()) } returns AddEditTaskResultDto(
            successfulRequestIds = listOf("req1", "req2"),
            deleteTaskAssignmentsTaskIds = listOf("task1")
        )

        application {
            configureSerialization()
            configureSecurity(
                jwtService,
                Constants.JWT_REALM,
                Constants.JWT_AUTH_CONFIG_BASE
            )
            TaskRoutes(accessController, localDateTimeConverter).register(this)
        }

        val jsonBody = """
            [
              {
                "type": "add",
                "name": "Task 1",
                "description": "Desc 1",
                "dueDateTime": "2025-01-01T10:00:00",
                "houseId": "house1",
                "memberId": "member1",
                "clientRequestId": "req1"
              },
              {
                "type": "edit",
                "taskId": "task1",
                "name": "Updated Task",
                "clientRequestId": "req2"
              }
            ]
        """.trimIndent()

        val response = client.post("/tasks/batch") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("req1"))
        assertTrue(responseText.contains("req2"))
    }

    @Test
    fun batch_shouldReturnPartialSuccess() = testApplication {
        val memberId = "member1"
        val member = Member(id = memberId, name = "Test", createdDate = Instant.now())
        val token = jwtService.generateToken(member)

        coEvery { accessController.addOrEdit(memberId, any()) } returns AddEditTaskResultDto(
            successfulRequestIds = listOf("req1"),
            deleteTaskAssignmentsTaskIds = emptyList()
        )

        application {
            configureSerialization()
            configureSecurity(
                jwtService,
                Constants.JWT_REALM,
                Constants.JWT_AUTH_CONFIG_BASE
            )
            TaskRoutes(accessController, localDateTimeConverter).register(this)
        }

        val jsonBody = """
            [
              {
                "type": "add",
                "name": "Task 1",
                "description": "Desc 1",
                "dueDateTime": "2025-01-01T10:00:00",
                "houseId": "house1",
                "memberId": "member1",
                "clientRequestId": "req1"
              },
              {
                "type": "edit",
                "taskId": "nonExistentTask",
                "name": "Failed Task",
                "clientRequestId": "req2"
              }
            ]
        """.trimIndent()

        val response = client.post("/tasks/batch") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(jsonBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("req1"))
        assertTrue(!responseText.contains("req2"))
    }
}
