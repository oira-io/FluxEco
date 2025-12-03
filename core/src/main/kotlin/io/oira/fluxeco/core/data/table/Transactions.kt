package io.oira.fluxeco.core.data.table

import org.jetbrains.exposed.sql.Table

object Transactions : Table("transactions") {
    val id = integer("id") // Removed autoIncrement to support both sequential and random
    val playerUuid = varchar("player_uuid", 36)
    val type = varchar("type", 20)
    val amount = double("amount")
    val senderUuid = varchar("sender_uuid", 36)
    val receiverUuid = varchar("receiver_uuid", 36)
    val date = long("date")

    override val primaryKey = PrimaryKey(id)
}
