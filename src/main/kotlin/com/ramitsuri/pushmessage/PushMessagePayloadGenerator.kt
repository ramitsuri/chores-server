package com.ramitsuri.pushmessage

import com.ramitsuri.models.MemberAssignment
import com.ramitsuri.models.ProgressStatus
import com.ramitsuri.repository.interfaces.MemberAssignmentsRepository
import com.ramitsuri.repository.interfaces.PushMessageTokenRepository
import com.ramitsuri.repository.interfaces.TaskAssignmentsRepository
import com.ramitsuri.repository.interfaces.TasksRepository
import java.time.Instant

class PushMessagePayloadGenerator(
    private val tasksRepository: TasksRepository,
    private val taskAssignmentsRepository: TaskAssignmentsRepository,
    private val memberAssignmentsRepository: MemberAssignmentsRepository,
    private val pushMessageTokenRepository: PushMessageTokenRepository,
) {
    suspend fun getForTasks(taskIds: List<String>, now: Instant = Instant.now()): List<PushMessage> {
        // We want to send 1 message to a user per device, using a map to combine potential multiple messages
        val memberIdPushMessageMap = mutableMapOf<String, PushMessagePayload>()

        // For caching to not read from disk repeatedly
        val houseIdMemberAssignmentsMap = mutableMapOf<String, List<MemberAssignment>>()

        val tasks = tasksRepository.get(taskIds)
        tasks.forEach { task ->
            val houseId = task.houseId
            val memberAssignments =
                houseIdMemberAssignmentsMap[houseId] ?: memberAssignmentsRepository.getForHouse(houseId)
            houseIdMemberAssignmentsMap[houseId] = memberAssignments
            memberAssignments.forEach { memberAssignment ->
                memberIdPushMessageMap[memberAssignment.member.id] = PushMessagePayload.default()
            }
        }

        val pushMessageTokens = pushMessageTokenRepository.getOfMembers(memberIdPushMessageMap.keys.toList(), now)
        return pushMessageTokens.mapNotNull { pushMessageToken ->
            val payload = memberIdPushMessageMap[pushMessageToken.memberId]
            if (payload != null) {
                PushMessage(
                    recipientToken = pushMessageToken.token,
                    payload = payload
                )
            } else {
                null
            }
        }
    }

    suspend fun getForTaskAssignments(
        taskAssignmentIds: List<String>,
        now: Instant = Instant.now()
    ): List<PushMessage> {
        // We want to send 1 message to a user per device, using a map to combine potential multiple messages
        val memberIdPushMessageMap = mutableMapOf<String, PushMessagePayload>()

        // For caching to not read from disk repeatedly
        val houseIdMemberAssignmentsMap = mutableMapOf<String, List<MemberAssignment>>()

        val taskAssignments = taskAssignmentsRepository.get(taskAssignmentIds)
        taskAssignments.forEach { taskAssignment ->
            val houseId = taskAssignment.task.houseId
            val memberAssignments =
                houseIdMemberAssignmentsMap[houseId] ?: memberAssignmentsRepository.getForHouse(houseId)
            houseIdMemberAssignmentsMap[houseId] = memberAssignments
            memberAssignments.forEach { memberAssignment ->
                val wasAssignedToThisMember = memberAssignment.member.id == taskAssignment.member.id
                val wasStatusChangedByOtherMember = taskAssignment.statusByMember != taskAssignment.member.id
                val pushMessageForMember =
                    memberIdPushMessageMap[memberAssignment.member.id] ?: PushMessagePayload.default()
                memberIdPushMessageMap[memberAssignment.member.id] =
                    if (wasAssignedToThisMember && wasStatusChangedByOtherMember) {
                        when (taskAssignment.progressStatus) {
                            ProgressStatus.DONE -> {
                                pushMessageForMember.addToDone(taskAssignment.task.name)
                            }

                            ProgressStatus.WONT_DO -> {
                                pushMessageForMember.addToWontDo(taskAssignment.task.name)
                            }

                            else -> {
                                pushMessageForMember
                            }
                        }
                    } else {
                        pushMessageForMember
                    }
            }
        }

        val pushMessageTokens = pushMessageTokenRepository.getOfMembers(memberIdPushMessageMap.keys.toList(), now)
        return pushMessageTokens.mapNotNull { pushMessageToken ->
            val payload = memberIdPushMessageMap[pushMessageToken.memberId]
            if (payload != null) {
                PushMessage(
                    recipientToken = pushMessageToken.token,
                    payload = payload
                )
            } else {
                null
            }
        }
    }
}