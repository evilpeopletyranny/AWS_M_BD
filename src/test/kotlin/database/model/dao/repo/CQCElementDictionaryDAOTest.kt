package database.model.dao.repo

import database.model.dao.entity.CQCElementDictionaryEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Тест DAO для доступа к словарю элементов ККХ
 */
class CQCElementDictionaryDAOTest : IDAOTest {

    private val defValues = setOf(
        CQCElementDictionaryEntity(UUID.randomUUID(), "Компетенция"),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Индикатор"),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Знание"),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Умение"),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Навык"),
    )

    /**
     * Выборка записи по id
     */
    @Test
    override fun `select by id`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = defValues.first()
            val resId = CQCElementDictionaryDAO.insert(entity)
            val res = resId?.let { CQCElementDictionaryDAO.selectById(it) }

            assertEquals(entity.id, resId)
            assertTrue { res != null }
            assertEquals(res, entity)

            rollback()
        }
    }

    /**
     * Выборка всех записей без параметров поиска
     */
    @Test
    override fun `select all without parameters`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.multiInsert(defValues)

            val res = CQCElementDictionaryDAO.selectAll(orderBy = "id")

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, defValues.size)
            assertEquals(defValues, res)

            rollback()
        }
    }

    /**
     * Выборка записей ограниченного размера
     */
    @Test
    override fun `select all with limit`() {
        val limit = 3

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.multiInsert(defValues)

            val res = CQCElementDictionaryDAO.selectAll(orderBy = "id", limit = limit)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(defValues.sortedBy { it.id.toString() }.take(limit), res.sortedBy { it.id.toString() })

            rollback()
        }
    }

    /**
     * Выборка со всеми параметрами поиска
     */
    @Test
    override fun `select all with all search options`() {
        val limit = 3
        val offset = 1
        val orderBy = "name"
        val order = "DESC"

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.multiInsert(defValues)

            val res = CQCElementDictionaryDAO.selectAll(limit, offset.toLong(), orderBy, order)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(
                defValues.sortedBy { it.name }.reversed().subList(offset, limit + offset).toSet(), res
            )

            rollback()
        }
    }

    /**
     * Успешное создание записи в таблице
     */
    @Test
    override fun `entity successfully created`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = defValues.first()

            val id = CQCElementDictionaryDAO.insert(entity)
            val res = CQCElementDictionaryDAO.selectById(entity.id)

            assertEquals(entity.id, id)
            assertTrue { res != null }
            assertEquals(res, entity)

            rollback()
        }
    }

    /**
     * Успешное обновление записи в таблице
     */
    @Test
    override fun `entity updated successfully`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = defValues.first()

            val id = CQCElementDictionaryDAO.insert(entity) ?: throw SQLException("Entity not created")
            val fromBD = CQCElementDictionaryDAO.selectById(entity.id)

            val forUpdate = CQCElementDictionaryEntity(
                id, "NEW NAME"
            )
            val updated = CQCElementDictionaryDAO.update(forUpdate)
            val res = CQCElementDictionaryDAO.selectById(id)

            assertTrue { res != null }
            assertEquals(forUpdate.id, id)
            assertEquals(updated, 1)
            assertEquals(res, forUpdate)
            assertNotEquals(res, fromBD)

            rollback()
        }
    }

    /**
     * Успешное удаление записи в таблице
     */
    @Test
    override fun `entity deleted successfully`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = defValues.first()

            val id = CQCElementDictionaryDAO.insert(entity)
            val fromBD = CQCElementDictionaryDAO.selectById(entity.id)
            val deleted = fromBD?.let { CQCElementDictionaryDAO.deleteById(it.id) }
            val res = fromBD?.let { CQCElementDictionaryDAO.selectById(it.id) }

            assertEquals(entity.id, id)
            assertEquals(deleted, 1)
            assertTrue { res == null }

            rollback()
        }
    }

    /**
     * Неудачное создание записи, поскольку данное имя уже занято
     */
    @Test
    fun `element not created because such name exists`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity1 = defValues.first()
            val entity2 = CQCElementDictionaryEntity(UUID.randomUUID(), entity1.name)

            assertThrows<SQLException> {
                CQCElementDictionaryDAO.multiInsert(listOf(entity1, entity2))
            }

            rollback()
        }
    }

    /**
     * Неудачное обновление записи, поскольку данное имя уже занято
     */
    @Test
    fun `element not updated because such name exists`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity1 = defValues.first()
            val entity2 = defValues.last()

            CQCElementDictionaryDAO.multiInsert(listOf(entity1, entity2))

            val fromBD = CQCElementDictionaryDAO.selectById(entity2.id)

            fromBD?.let {
                val forUpdate = CQCElementDictionaryEntity(it.id, entity1.name)

                assertThrows<SQLException> {
                    CQCElementDictionaryDAO.update(forUpdate)
                }
            }

            rollback()
        }
    }
}