package com.ramitsuri.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime

@Serializable
data class House(
    val id: String,
    val name: String,
    val createdByMemberId: String,
    @Serializable(with = InstantSerializer::class)
    val createdDate: Instant,
    @Serializable(with = ActiveStatusSerializer::class)
    val status: ActiveStatus,
)

@Serializable
data class HouseDto(
    val name: String,
    val createdByMemberId: String?,
)

@Serializable
data class Member(
    val id: String,
    val name: String,
    @Serializable(with = InstantSerializer::class)
    val createdDate: Instant,
    val key: String = "",
    @Serializable(with = ResourceAccessSerializer::class)
    val access: Access = Access.READ_HOUSE_WRITE_OWN,
)

@Serializable
data class MemberDto(val name: String)

@Serializable
data class MemberAssignment(
    val id: String,
    val member: Member,
    val houseId: String,
)

@Serializable
data class MemberAssignmentDto(
    val memberId: String,
    val houseId: String,
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
    @Serializable(with = LocalDateTimeSerializer::class)
    val repeatEndDateTime: LocalDateTime?,
    val houseId: String,
    val memberId: String,
    val rotateMember: Boolean,
    @Serializable(with = InstantSerializer::class)
    val createdDate: Instant,
    @Serializable(with = ActiveStatusSerializer::class)
    val status: ActiveStatus,
)

@Serializable
data class TaskDto(
    val name: String?,
    val description: String?,
    val dueDateTime: String?,
    val repeatValue: Int?,
    val repeatUnit: Int?,
    val repeatEndDateTime: String?,
    val houseId: String?,
    val memberId: String?,
    val rotateMember: Boolean?,
    val status: Int?,
)

@Serializable
sealed interface AddEditTaskDto {
    val name: String?
    val description: String?
    val dueDateTime: String?
    val repeatValue: Int?
    val repeatUnit: Int?
    val repeatEndDateTime: String?
    val houseId: String?
    val memberId: String?
    val rotateMember: Boolean?
    val status: Int?
    val clientRequestId: String

    @Serializable
    @SerialName("add")
    data class Add(
        override val name: String? = null,
        override val description: String? = null,
        override val dueDateTime: String? = null,
        override val repeatValue: Int? = null,
        override val repeatUnit: Int? = null,
        override val repeatEndDateTime: String? = null,
        override val houseId: String? = null,
        override val memberId: String? = null,
        override val rotateMember: Boolean? = null,
        override val status: Int = ActiveStatus.ACTIVE.key,
        override val clientRequestId: String,
    ) : AddEditTaskDto {
        val createdDate: Instant
            get() = Instant.now()
    }

    @Serializable
    @SerialName("edit")
    data class Edit(
        val taskId: String,
        override val name: String? = null,
        override val description: String? = null,
        override val dueDateTime: String? = null,
        override val repeatValue: Int? = null,
        override val repeatUnit: Int? = null,
        override val repeatEndDateTime: String? = null,
        override val houseId: String? = null,
        override val memberId: String? = null,
        override val rotateMember: Boolean? = null,
        override val status: Int? = null,
        override val clientRequestId: String,
    ) : AddEditTaskDto {
        val shouldDeleteTaskAssignments = dueDateTime != null ||
                repeatUnit != null ||
                repeatValue != null ||
                repeatEndDateTime != null ||
                rotateMember != null ||
                status != null
    }
}

@Serializable
data class AddEditTaskResultDto(
    val successfulRequestIds: List<String>,
    val deleteTaskAssignmentsTaskIds: List<String>,
    val tasks: List<Task>,
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
    val createType: CreateType,
    val statusByMember: String?,
)

@Serializable
data class TaskAssignmentDto(
    val id: String = "",
    val progressStatus: Int,
    @Serializable(with = InstantSerializer::class)
    val progressStatusDate: Instant = Instant.now(),
)

@Serializable
data class PushMessageToken(
    val memberId: String,
    val deviceId: String,
    val token: String,
    @Serializable(with = InstantSerializer::class)
    val uploadDateTime: Instant,
)

@Serializable
data class PushMessageTokenDto(
    val deviceId: String,
    val token: String,
)

@Serializable
data class Error(
    @Serializable(with = ErrorCodeSerializer::class)
    val code: ErrorCode,
    val message: String,
)

@Serializable
data class Token(val authToken: String)

@Serializable
data class SyncResult(
    val associatedLists: List<House>,
    val memberListAssociations: List<MemberAssignment>,
)

@Serializable
data class LoginParam(val id: String?, val key: String?)

sealed class AccessResult<out T> {
    data class Success<T>(val data: T) : AccessResult<T>()
    data object Failure : AccessResult<Nothing>()
}
