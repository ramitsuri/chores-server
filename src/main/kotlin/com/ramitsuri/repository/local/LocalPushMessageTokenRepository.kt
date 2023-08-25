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
import java.util.UUID

class LocalPushMessageTokenRepository(
    private val uuidConverter: Converter<String, UUID>
) : PushMessageTokenRepository {

    override suspend fun getForMember(memberId: String): List<PushMessageToken> {
        return DatabaseFactory.query {
            val memberIdUuid = uuidConverter.toStorage(memberId)
            PushMessageTokens
                .select { PushMessageTokens.memberId.eq(memberIdUuid) }
                .filterNotNull()
                .map { row ->
                    rowToPushMessageToken(row)
                }
        }
    }

    override suspend fun addOrReplace(memberId: String, deviceId: String, token: String): PushMessageToken? {
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
        return PushMessageToken(memberId = memberId, deviceId = deviceId, token = token)
    }
}