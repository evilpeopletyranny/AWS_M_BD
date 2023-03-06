package database.dao

import database.DatabaseFactory
import org.junit.jupiter.api.BeforeAll

//TODO("SL4J)"

/**
 * Общий интерфейс тестирования объекта доступа к таблице
 */
sealed interface IDAOTest {
    /**
     * Выборка элемента по id
     */
    fun `select by id`()

    /**
     * Выборка всех элементов без параметров поиска
     */
    fun `select all without parameters`()

    /**
     * Выборка элементов ограниченного размера
     */
    fun `select all with limit`()

    /**
     * Выборка элементов со всеми параметрами поиска
     */
    fun `select all with all search options`()

    /**
     * Успешное добавление записи
     */
    fun `entity successfully created`()

    /**
     * Успешное обновление записи
     */
    fun `entity updated successfully`()

    /**
     * Успешное удаление записи
     */
    fun `entity deleted successfully`()

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