package database.dao

import database.DatabaseFactory
import org.junit.jupiter.api.BeforeAll

//TODO("SL4J)"

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
            DatabaseFactory.init(
                url = """jdbc:postgresql://localhost:5432/cqc_test?
                        reWriteBatchedInserts=true& +
                        rewriteBatchedStatements=true&
                        shouldReturnGeneratedValues=false"""
            )
        }
    }
}