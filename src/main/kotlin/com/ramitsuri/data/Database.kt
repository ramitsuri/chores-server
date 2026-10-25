package com.ramitsuri.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.Database.Companion.connect
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

object DatabaseFactory {
    private var dispatcher: CoroutineDispatcher = Dispatchers.IO

    fun init(
        url: String,
        driver: String,
        username: String = "",
        password: String = "",
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) {
        this.dispatcher = dispatcher
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
        return withContext(dispatcher) {
            transaction {
                block()
            }
        }
    }

    suspend fun <T> queryWithTransaction(block: (JdbcTransaction) -> T): T {
        return withContext(dispatcher) {
            transaction {
                block(this)
            }
        }
    }
}

object Houses : UUIDTable() {
    val name: Column<String> = varchar("name", 50)
    val createdByMemberId: Column<UUID> = javaUUID("memberId").references(Members.id)
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
    val memberId: Column<UUID> = javaUUID("memberId").references(Members.id)
    val houseId: Column<UUID> = javaUUID("houseId").references(Houses.id)
}

object Tasks : UUIDTable() {
    val name: Column<String> = varchar("name", 50)
    val description: Column<String> = varchar("description", 250)
    val dueDate: Column<String> = varchar("dueDate", 50)
    val repeatValue: Column<Int> = integer("repeatValue")
    val repeatUnit: Column<Int> = integer("repeatUnit")
    val repeatEndDate: Column<String?> = varchar("repeatEndDate", 50).nullable().default(null)
    val houseId: Column<UUID> = javaUUID("houseId").references(Houses.id)
    val memberId: Column<UUID> = javaUUID("memberId").references(Members.id)
    val rotateMember: Column<Boolean> = bool("rotateMember")
    val createdDate: Column<String> = varchar("createdDate", 50)
    val activeStatus: Column<Int> = integer("activeStatus").default(1)
}

object TaskAssignments : UUIDTable() {
    val statusType: Column<Int> = integer("statusType")
    val statusDate: Column<String> = varchar("statusDate", 50)
    val taskId: Column<UUID> = javaUUID("taskId").references(Tasks.id)
    val memberId: Column<UUID> = javaUUID("memberId").references(Members.id)
    val dueDate: Column<String> = varchar("dueDate", 50)
    val createdDate: Column<String> = varchar("createdDate", 50)
    val createType: Column<Int> = integer("createType")
    val statusByMember: Column<UUID?> = javaUUID("statusByMember").nullable().default(null)
}

object RunTimeLogs : IntIdTable() {
    val runTime: Column<String> = varchar("runDate", 50)
}

object PushMessageTokens : IntIdTable() {
    val memberId: Column<UUID> = javaUUID("memberId").references(Members.id)
    val deviceId: Column<UUID> = javaUUID("deviceId")
    val token: Column<String> = text("token")
    val uploadedDateTime: Column<String> = varchar("uploadedDateTime", 50).default("2023-09-13T00:00:00Z")
}
