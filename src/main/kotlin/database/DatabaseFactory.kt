package database

import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

/**
 * Объект конфигурации БД
 */
object DatabaseFactory {
    /**
     * Инициализация подключения к БД
     *
     * @param url подключения к БД
     */
    fun init(url: String) {
        val driverClass = "org.postgresql.Driver"
        val user = "postgres"
        val password = "postgre"

        val hikariDataSource = HikariDataSource()
        hikariDataSource.driverClassName = driverClass
        hikariDataSource.jdbcUrl = url
        hikariDataSource.username = user
        hikariDataSource.password = password

        Database.connect(hikariDataSource)
    }
}