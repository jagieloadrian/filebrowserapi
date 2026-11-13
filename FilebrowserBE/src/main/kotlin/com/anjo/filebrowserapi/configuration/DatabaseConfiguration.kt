package com.anjo.filebrowserapi.configuration

import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration {
    @Bean
    fun database(dataSource: DataSource): Database = Database.connect(dataSource)
}
