package com.ramitsuri.models

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Serializable
data class House(
    val id: String,
    val name: String,
    val createdByMemberId: String,
    @Serializable(with = InstantSerializer::class)
    val createdDate: Instant,
    @Serializable(with = ActiveStatusSerializer::class)
    val status: ActiveStatus
)

@Serializable
data class HouseDto(
    val name: String,
    val createdByMemberId: String?
)

@Serializable
data class Member(
    val id: String,
    val name: String,
    @Serializable(with = InstantSerializer::class)
    val createdDate: Instant,
    val key: String = "",
    @Serializable(with = ResourceAccessSerializer::class)
    val access: Access = Access.READ_HOUSE_WRITE_OWN
)

@Serializable
data class MemberDto(val name: String)

@Serializable
data class MemberAssignment(
    val id: String,
    val member: Member,
    val houseId: String
)

@Serializable
data class MemberAssignmentDto(
    val memberId: String,
    val houseId: String
)

@Serializable
data class Task(
    val id: String,
    val name: String,
    val description: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val dueDateTime: LocalDateTime,
    val repeatValue: Int,
    @Serializable(with = RepeatUnitSerializer::class)
    val repeatUnit: RepeatUnit,
    val houseId: String,
    val memberId: String,
    val rotateMember: Boolean,
    @Serializable(with = InstantSerializer::class)
    val createdDate: Instant,
    @Serializable(with = ActiveStatusSerializer::class)
    val status: ActiveStatus
)

@Serializable
data class TaskDto(
    val name: String?,
    val description: String?,
    val dueDateTime: String?,
    val repeatValue: Int?,
    val repeatUnit: Int?,
    val houseId: String?,
    val memberId: String?,
    val rotateMember: Boolean?,
    val status: ActiveStatus?
)

@Serializable
data class TaskAssignment(
    val id: String,
    @Serializable(with = ProgressStatusSerializer::class)
    val progressStatus: ProgressStatus,
    @Serializable(with = InstantSerializer::class)
    val progressStatusDate: Instant,
    val task: Task,
    val member: Member,
    @Serializable(with = LocalDateTimeSerializer::class)
    val dueDateTime: LocalDateTime,
    @Serializable(with = InstantSerializer::class)
    val createdDate: Instant,
    @Serializable(with = CreateTypeSerializer::class)
    val createType: CreateType
)

@Serializable
data class TaskAssignmentDto(
    val id: String = "",
    val progressStatus: Int,
    @Serializable(with = InstantSerializer::class)
    val progressStatusDate: Instant = Instant.now()
)

@Serializable
data class Error(
    @Serializable(with = ErrorCodeSerializer::class)
    val code: ErrorCode, val message: String
)

@Serializable
data class Token(val authToken: String)

@Serializable
data class SyncResult(
    val associatedLists: List<House>
)

data class RepeatSchedulerConfig(
    val repeatType: SchedulerRepeatType,
    val zoneId: ZoneId
)

@Serializable
data class LoginParam(val id: String?, val key: String?)

sealed class AccessResult<out T> {
    data class Success<T>(val data: T) : AccessResult<T>()
    object Failure : AccessResult<Nothing>()
}