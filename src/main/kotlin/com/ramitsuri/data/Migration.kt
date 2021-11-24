package com.ramitsuri.data

import com.ramitsuri.repository.local.*
import com.ramitsuri.repository.remote.*
import java.lang.Exception

class Migration(
    private val localMembersRepository: LocalMembersRepository,
    private val remoteMembersRepository: RemoteMembersRepository,
    private val localHousesRepository: LocalHousesRepository,
    private val remoteHousesRepository: RemoteHousesRepository,
    private val localMemberAssignmentsRepository: LocalMemberAssignmentsRepository,
    private val remoteMemberAssignmentsRepository: RemoteMemberAssignmentsRepository,
    private val localTasksRepository: LocalTasksRepository,
    private val remoteTasksRepository: RemoteTasksRepository,
    private val localTaskAssignmentsRepository: LocalTaskAssignmentsRepository,
    private val remoteTaskAssignmentsRepository: RemoteTaskAssignmentsRepository
) {

    suspend fun run(): Boolean {
        return try {
            migrateMembers()
            migrateHouses()
            migrateMemberAssignments()
            migrateTasks()
            migrateTaskAssignments()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun counts(): String {
        val countMembersRemote = remoteMembersRepository.rows()
        val countMembersLocal = localMembersRepository.rows()

        val countHousesRemote = remoteHousesRepository.rows()
        val countHousesLocal = localHousesRepository.rows()

        val countMemberAssignmentsRemote = remoteMemberAssignmentsRepository.rows()
        val countMemberAssignmentsLocal = localMemberAssignmentsRepository.rows()

        val countTasksRemote = remoteTasksRepository.rows()
        val countTasksLocal = localTasksRepository.rows()

        val countTaskAssignmentsRemote = remoteTaskAssignmentsRepository.rows()
        val countTaskAssignmentsLocal = localTaskAssignmentsRepository.rows()

        return """
            Count Local Members: $countMembersLocal
            Count Remote Members: $countMembersRemote
            ---------------------------------
            Count Local Houses: $countHousesLocal
            Count Remote Houses: $countHousesRemote
            ---------------------------------
            Count Local MemberAssignments: $countMemberAssignmentsLocal
            Count Remote MemberAssignments: $countMemberAssignmentsRemote
            ---------------------------------
            Count Local Tasks: $countTasksLocal
            Count Remote Tasks: $countTasksRemote
            ---------------------------------
            Count Local TaskAssignments: $countTaskAssignmentsLocal
            Count Remote TaskAssignments: $countTaskAssignmentsRemote
        """.trimIndent()
    }

    private suspend fun migrateMembers() {
        val members = remoteMembersRepository.get()
        localMembersRepository.add(members)
    }

    private suspend fun migrateHouses() {
        val houses = remoteHousesRepository.get()
        localHousesRepository.add(houses)
    }

    private suspend fun migrateMemberAssignments() {
        val memberAssignments = remoteMemberAssignmentsRepository.get()
        localMemberAssignmentsRepository.add(memberAssignments)
    }

    private suspend fun migrateTasks() {
        val tasks = remoteTasksRepository.get()
        localTasksRepository.add(tasks)
    }

    private suspend fun migrateTaskAssignments() {
        val taskAssignments = remoteTaskAssignmentsRepository.get()
        localTaskAssignmentsRepository.add(taskAssignments)
    }
}