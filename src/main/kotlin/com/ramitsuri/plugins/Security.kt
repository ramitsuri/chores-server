package com.ramitsuri.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import com.ramitsuri.models.Error
import com.ramitsuri.models.ErrorCode
import com.ramitsuri.models.Member
import com.ramitsuri.repository.interfaces.MembersRepository
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.util.*

fun Application.configureSecurity(jwtService: JwtService, realm: String, authenticationConfig: String) {
    authentication {
        jwt(authenticationConfig) {
            this.realm = realm
            verifier(jwtService.verifier)
            validate { credential ->
                jwtService.validator(credential)
            }
            /*challenge { _, _ ->
                jwtService.challenge()
            }*/
        }
    }
}

class JwtService(
    private val issuer: String,
    private val membersRepository: MembersRepository,
    private val tokenExpiration: Duration,
    secret: String
) {
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()

    fun generateToken(member: Member): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("id", member.id)
        .withExpiresAt(getExpiresAt())
        .sign(algorithm)

    fun challenge(): suspend PipelineContext<*, ApplicationCall>.(defaultScheme: String, realm: String) -> Unit =
        { _, _ ->
            val authToken = call.request.headers["Authorization"]
            val error = decodeToken(authToken)
            call.respond(
                HttpStatusCode.Unauthorized,
                error
            )
        }

    private fun decodeToken(token: String?): Error {
        token?.let {
            if (it.isEmpty()) {
                return Error(
                    ErrorCode.EMPTY_TOKEN,
                    "Token not provided"
                )
            }
            val strippedToken = it.replace("Bearer ", "")
            return try {
                val tokenParts = strippedToken.split(".")
                val body = String(Base64.getUrlDecoder().decode(tokenParts[1]))
                val tokenBody = Gson().fromJson(body, TokenBody::class.java)
                if (tokenBody.exp < Instant.now().toEpochMilli()) {
                    Error(
                        ErrorCode.EXPIRED_TOKEN,
                        "Token is expired"
                    )
                } else {
                    Error(
                        ErrorCode.INVALID_TOKEN,
                        "Token is invalid"
                    )
                }
            } catch (e: Exception) {
                Error(
                    ErrorCode.INVALID_TOKEN,
                    "Token is invalid"
                )
            }
        } ?: return Error(
            ErrorCode.MISSING_AUTHORIZATION,
            "Missing authorization header"
        )
    }

    suspend fun validator(credential: JWTCredential): JWTPrincipal? {
        val memberId = credential.payload.getClaim("id")?.asString() ?: return null
        membersRepository.get(memberId) ?: return null
        return JWTPrincipal(credential.payload)
    }

    private fun getExpiresAt() = Date(System.currentTimeMillis() + tokenExpiration.toMillis())
}

@Serializable
data class TokenBody(val sub: String, val iss: String, val id: String, val exp: Long)

fun ApplicationCall.getMemberId(): String? {
    val principal = principal<JWTPrincipal>()
    return principal?.getClaim("id", String::class)
}