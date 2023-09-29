package com.ramitsuri.repository.local

import com.ramitsuri.data.Converter
import com.ramitsuri.data.DatabaseFactory
import com.ramitsuri.data.PushMessageTokens
import com.ramitsuri.models.PushMessageToken
import com.ramitsuri.repository.interfaces.PushMessageTokenRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.time.Duration
import java.time.Instant
import java.util.UUID

class LocalPushMessageTokenRepository(
    private val uuidConverter: Converter<String, UUID>,
    private val instantConverter: Converter<Instant, String>,
) : PushMessageTokenRepository {

    override suspend fun getOfMembers(memberIds: List<String>, now: Instant): List<PushMessageToken> {
        return DatabaseFactory.query {
            val memberIdUuids = memberIds.map { uuidConverter.toStorage(it) }
            PushMessageTokens
                .select { PushMessageTokens.memberId.inList(memberIdUuids) }
                .filterNotNull()
                .map { row ->
                    rowToPushMessageToken(row)
                }
                .filter { Duration.between(it.uploadDateTime, now) < Duration.ofDays(30) }
        }
    }

    override suspend fun addOrReplace(
        memberId: String,
        deviceId: String,
        token: String,
        addedDateTime: Instant
    ): PushMessageToken? {
        return DatabaseFactory.query {
            val memberIdUuid = uuidConverter.toStorage(memberId)
            val deviceIdUuid = uuidConverter.toStorage(deviceId)
            val existing = PushMessageTokens
                .select {
                    PushMessageTokens.memberId.eq(memberIdUuid)
                        .and(PushMessageTokens.deviceId.eq(deviceIdUuid))
                }
                .filterNotNull()
                .map { row ->
                    rowToPushMessageToken(row)
                }

            if (existing.isNotEmpty()) {
                PushMessageTokens.deleteWhere {
                    PushMessageTokens.memberId.eq(memberIdUuid)
                        .and(PushMessageTokens.deviceId.eq(deviceIdUuid))
                }
            }
            val statement = PushMessageTokens.insert { pushMessageToken ->
                pushMessageToken[PushMessageTokens.memberId] = uuidConverter.toStorage(memberId)
                pushMessageToken[PushMessageTokens.deviceId] = uuidConverter.toStorage(deviceId)
                pushMessageToken[PushMessageTokens.token] = token
                pushMessageToken[PushMessageTokens.uploadedDateTime] = instantConverter.toStorage(addedDateTime)
            }
            statement.resultedValues?.get(0)?.let {
                rowToPushMessageToken(it)
            }
        }
    }

    private fun rowToPushMessageToken(row: ResultRow): PushMessageToken {
        val memberId = uuidConverter.toMain(row[PushMessageTokens.memberId])
        val deviceId = uuidConverter.toMain(row[PushMessageTokens.deviceId])
        val token = row[PushMessageTokens.token]
        val addedDateTime = instantConverter.toMain(row[PushMessageTokens.uploadedDateTime])
        return PushMessageToken(
            memberId = memberId,
            deviceId = deviceId,
            token = token,
            uploadDateTime = addedDateTime
        )
    }
}