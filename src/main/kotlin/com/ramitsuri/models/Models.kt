package com.ramitsuri.models

import kotlinx.serialization.Serializable
import java.time.Instant
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
    val key: String = ""
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
    @Serializable(with = InstantSerializer::class)
    val dueDateTime: Instant,
    val repeatValue: Int,
    @Serializable(with = RepeatUnitSerializer::class)
    val repeatUnit: RepeatUnit,
    val houseId: String,
    val memberId: String,
    val rotateMember: Boolean,
    @Serializable(with = InstantSerializer::class)
    val createdDate: Instant
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
    val rotateMember: Boolean?
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
    @Serializable(with = InstantSerializer::class)
    val dueDateTime: Instant,
    @Serializable(with = InstantSerializer::class)
    val createdDate: Instant,
    @Serializable(with = CreateTypeSerializer::class)
    val createType: CreateType
)

@Serializable
data class TaskAssignmentDto(
    val id: String?,
    val progressStatus: Int,
    @Serializable(with = InstantSerializer::class)
    val progressStatusDate: Instant?
)

@Serializable
data class Error(
    @Serializable(with = ErrorCodeSerializer::class)
    val code: ErrorCode, val message: String
)

@Serializable
data class Token(val authToken: String)

data class RepeatSchedulerConfig(
    val repeatType: SchedulerRepeatType,
    val zoneId: ZoneId
)

@Serializable
data class LoginParam(val id: String?, val key: String?)