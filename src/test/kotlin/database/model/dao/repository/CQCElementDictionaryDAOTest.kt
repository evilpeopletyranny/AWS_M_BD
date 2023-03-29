package database.model.dao.repository

import database.model.dao.entity.CQCElementDictionaryEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@DisplayName("Interaction test with cqc_elem_dict table")
class CQCElementDictionaryDAOTest : IDAOTest {
    private val orderBy = "id"

    private val defValues = setOf(
        CQCElementDictionaryEntity(UUID.randomUUID(), "Компетенция", false),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Индикатор", true),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Знание", false),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Умение", true),
        CQCElementDictionaryEntity(UUID.randomUUID(), "Навык", false),
    )

    @Test
    @DisplayName("Successful insertion of one record")
    fun `entity successfully created`() {
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

    @Test
    @DisplayName("Unsuccessful insertion of one record - such name exists")
    fun `element not created because such name exists`() {
        transaction {
            addLogger(StdOutSqlLogger)

            val entity1 = defValues.first()
            val entity2 = CQCElementDictionaryEntity(UUID.randomUUID(), entity1.name, entity1.isDeleted)

            CQCElementDictionaryDAO.insert(entity1)

            assertThrows<SQLException> {
                CQCElementDictionaryDAO.insert(entity2)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful insertion of multiple values")
    fun `successful creation of an entity set`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementDictionaryDAO.multiInsert(defValues)
            val res = CQCElementDictionaryDAO.selectAll(orderBy = orderBy)

            assertTrue { res.isNotEmpty() }
            assertEquals(defValues, res)

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion of multiple values - such name exists")
    fun `entity set was not created, because such name exist`() {
        transaction {
            addLogger(StdOutSqlLogger)

            val entity = defValues.first()
            CQCElementDictionaryDAO.insert(entity)

            assertThrows<SQLException> {
                CQCElementDictionaryDAO.multiInsert(defValues)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful select all elements without search parameters")
    fun `select all without parameters`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementDictionaryDAO.multiInsert(defValues)
            val res = CQCElementDictionaryDAO.selectAll(orderBy = orderBy)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, defValues.size)
            assertEquals(defValues, res)

            rollback()
        }
    }

    @Test
    @DisplayName("Successfully select a limited number of elements")
    fun `select limited number elements`() {
        val limit = 3

        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementDictionaryDAO.multiInsert(defValues)

            val res = CQCElementDictionaryDAO.selectAll(limit, orderBy = orderBy)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(
                defValues.sortedBy { it.id.toString() }.take(limit).toSet(), res
            )

            rollback()
        }
    }

    @Test
    @DisplayName("Successfully select with all search options")
    fun `select all with all search options`() {
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

    @Test
    @DisplayName("Successfully select of one record")
    fun `select by id`() {
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

    @Test
    @DisplayName("Unsuccessfully select of one record - non-existent id")
    fun `unsuccessfully select by id`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementDictionaryDAO.multiInsert(defValues)
            val res = CQCElementDictionaryDAO.selectById(UUID.randomUUID())

            assertTrue { res == null }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful update of one entity")
    fun `successfully entity update`() {
        transaction {
            addLogger(StdOutSqlLogger)

            val entity = defValues.find { it.name == "Компетенция" }!!

            val id = CQCElementDictionaryDAO.insert(entity) ?: throw SQLException("Entity not created")
            val fromBD = CQCElementDictionaryDAO.selectById(entity.id) ?: throw SQLException("Entity not created")

            val forUpdate = CQCElementDictionaryEntity(
                fromBD.id, "NEW NAME", true
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
    @DisplayName("Unsuccessful update of one entity - such name exists")
    fun `element not updated because such name exists`() {
        transaction {
            addLogger(StdOutSqlLogger)

            val entity1 = defValues.first()
            val entity2 = defValues.last()

            CQCElementDictionaryDAO.multiInsert(listOf(entity1, entity2))

            val fromBD = CQCElementDictionaryDAO.selectById(entity2.id)

            fromBD?.let {
                val forUpdate = CQCElementDictionaryEntity(it.id, entity1.name, entity1.isDeleted)

                assertThrows<SQLException> {
                    CQCElementDictionaryDAO.update(forUpdate)
                }
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful deletion of the record")
    fun `entity deleted successfully`() {
        transaction {
            addLogger(StdOutSqlLogger)

            val entity = defValues.first()
            val expectedRes = defValues.filterNot { it == entity }.toSet()

            CQCElementDictionaryDAO.multiInsert(defValues)
            val deleted = CQCElementDictionaryDAO.deleteById(entity.id)
            val res = CQCElementDictionaryDAO.selectAll(orderBy = orderBy)

            assertEquals(deleted, 1)
            assertEquals(expectedRes, res)

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessfully deletion - record does not exist")
    fun `Unsuccessfully record deletion - record does not exist`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementDictionaryDAO.multiInsert(defValues)
            val deleted = CQCElementDictionaryDAO.deleteById(UUID.randomUUID())

            assertEquals(deleted, 0)

            rollback()
        }
    }
}