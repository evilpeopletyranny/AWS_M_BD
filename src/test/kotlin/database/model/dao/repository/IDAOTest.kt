package database.model.dao.repository

import database.DatabaseFactory
import org.junit.jupiter.api.BeforeAll

//TODO("SL4J)"

/**
 * Общий интерфейс тестирования объекта доступа к таблице
 */
interface IDAOTest {

    companion object {
        /**
         * Инициализация подключения к БД перед тестами
         */
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