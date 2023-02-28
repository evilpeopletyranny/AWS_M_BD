package database.dao

import database.entity.CQCElementDictionaryEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CQCElementDictionaryTest : IDAOTest {

    private val defValues = setOf(
        CQCElementDictionaryEntity(UUID.randomUUID(), "Компетенция"),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Индикатор"),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Знание"),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Умение"),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Навык"),
    )

    //TODO("SL4J)"

    @Test
    override fun `select all without parameters`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.multiInsert(defValues)

            val res = CQCElementDictionaryDAO.selectAll()

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, defValues.size)
            assertEquals(defValues, res)

            rollback()
        }
    }

    @Test
    override fun `select all with limit`() {
        val limit = 3

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.multiInsert(defValues)

            val res = CQCElementDictionaryDAO.selectAll(limit)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(defValues.sortedBy { it.id.toString() }.take(limit), res.sortedBy { it.id.toString() })

            rollback()
        }
    }

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
                defValues.sortedBy { it.name }.reversed().subList(offset, limit + offset).toSet(),
                res
            )

            rollback()
        }
    }

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

    override fun `entity updated successfully`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = defValues.first()

            val id = CQCElementDictionaryDAO.insert(entity)
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
}