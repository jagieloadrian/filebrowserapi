package com.anjo.filebrowserapi.repository

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object TestDatabase {
    val db: Database by lazy {
        val dataSource = HikariDataSource().apply { jdbcUrl = "jdbc:tc:postgresql:17-alpine:///filebrowser?TC_DAEMON=true" }
        Flyway
            .configure()
            .dataSource(dataSource)
            .load()
            .migrate()
        Database.connect(dataSource)
    }

    fun clean() {
        transaction(db) {
            listOf(EntryMetaTable, FileHashTable, FileVersionTable, ShareLinkTable, TrashItemTable).forEach { it.deleteAll() }
        }
    }
}
