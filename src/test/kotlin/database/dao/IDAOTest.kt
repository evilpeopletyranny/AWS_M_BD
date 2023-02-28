package database.dao

import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll

/**
 * Общий интерфейс тестирования объекта доступа к таблице
 */
sealed interface IDAOTest {

    fun `select all without parameters`()

    fun `select all with limit`()

    fun `select all with all search options`()

    fun `entity successfully created`()

    fun `entity updated successfully`()

    fun `entity deleted successfully`()

    companion object {
        @JvmStatic
        @BeforeAll
        fun connect() {
            Database.connect(
                url = "jdbc:postgresql://localhost:5432/cqc_test",
                driver = "org.postgresql.Driver",
                user = "postgres",
                password = "postgre",
            )
        }
    }
}