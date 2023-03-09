package com.ramitsuri.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database.Companion.connect
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object DatabaseFactory {
    fun init(url: String, driver: String, username: String = "", password: String = "") {

        val database = connect(url, driver, username, password)
        TransactionManager.defaultDatabase = database

        transaction {
            for (table in getTables()) {
                SchemaUtils.createMissingTablesAndColumns(table)
            }
        }
    }

    fun clear() {
        transaction {
            for (table in getTables()) {
                SchemaUtils.drop(table)
            }
        }
    }

    private fun getTables(): List<Table> {
        return listOf(
            Houses,
            Members,
            MemberAssignments,
            Tasks,
            TaskAssignments,
            RunTimeLogs,
            PushMessageTokens
        )
    }

    suspend fun <T> query(block: () -> T): T {
        return withContext(Dispatchers.IO) {
            transaction {
                block()
            }
        }
    }

    suspend fun <T> queryWithTransaction(block: (Transaction) -> T): T {
        return withContext(Dispatchers.IO) {
            transaction {
                block(this)
            }
        }
    }
}

object Houses : UUIDTable() {
    val name: Column<String> = varchar("name", 50)
    val createdByMemberId: Column<UUID> = uuid("memberId").references(Members.id)
    val createdDate: Column<String> = varchar("createdDate", 50)
    val activeStatus: Column<Int> = integer("activeStatus")
}

object Members : UUIDTable() {
    val name: Column<String> = varchar("name", 50)
    val createdDate: Column<String> = varchar("createdDate", 50)
    val key: Column<String> = varchar("key", 80).default("")
    val access: Column<Int> = integer("access").default(1)
}

object MemberAssignments : UUIDTable() {
    val memberId: Column<UUID> = uuid("memberId").references(Members.id)
    val houseId: Column<UUID> = uuid("houseId").references(Houses.id)
}

object Tasks : UUIDTable() {
    val name: Column<String> = varchar("name", 50)
    val description: Column<String> = varchar("description", 250)
    val dueDate: Column<String> = varchar("dueDate", 50)
    val repeatValue: Column<Int> = integer("repeatValue")
    val repeatUnit: Column<Int> = integer("repeatUnit")
    val houseId: Column<UUID> = uuid("houseId").references(Houses.id)
    val memberId: Column<UUID> = uuid("memberId").references(Members.id)
    val rotateMember: Column<Boolean> = bool("rotateMember")
    val createdDate: Column<String> = varchar("createdDate", 50)
    val activeStatus: Column<Int> = integer("activeStatus").default(1)
}

object TaskAssignments : UUIDTable() {
    val statusType: Column<Int> = integer("statusType")
    val statusDate: Column<String> = varchar("statusDate", 50)
    val taskId: Column<UUID> = uuid("taskId").references(Tasks.id)
    val memberId: Column<UUID> = uuid("memberId").references(Members.id)
    val dueDate: Column<String> = varchar("dueDate", 50)
    val createdDate: Column<String> = varchar("createdDate", 50)
    val createType: Column<Int> = integer("createType")
    val statusByMember: Column<UUID?> = uuid("statusByMember").nullable().default(null)
}

object RunTimeLogs : IntIdTable() {
    val runTime: Column<String> = varchar("runDate", 50)
}

object PushMessageTokens: IntIdTable(){
    val memberId: Column<UUID> = uuid("memberId").references(Members.id)
    val deviceId: Column<UUID> = uuid("deviceId")
    val token: Column<String> = text("token")
}
