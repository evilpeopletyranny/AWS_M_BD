package database.dao

import database.entity.CQCElementDictionaryEntity
import database.entity.CQCElementHierarchyEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CQCElementHierarchyDAOTest : IDAOTest {
    private val orderBy = "child_type_id"

    companion object {
        enum class HierarchyElements {
            Competence, Indicator, Knowledge, Ability, Skill
        }

        private val dictionary = mapOf(
            HierarchyElements.Competence to CQCElementDictionaryEntity(
                UUID.randomUUID(), HierarchyElements.Competence.name
            ),
            HierarchyElements.Indicator to CQCElementDictionaryEntity(
                UUID.randomUUID(), HierarchyElements.Indicator.name
            ),
            HierarchyElements.Knowledge to CQCElementDictionaryEntity(
                UUID.randomUUID(), HierarchyElements.Knowledge.name
            ),
            HierarchyElements.Ability to CQCElementDictionaryEntity(UUID.randomUUID(), HierarchyElements.Ability.name),
            HierarchyElements.Skill to CQCElementDictionaryEntity(UUID.randomUUID(), HierarchyElements.Skill.name),
        )

        val hierarchy = setOf(
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Competence]!!.id, dictionary[HierarchyElements.Indicator]!!.id
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!.id, dictionary[HierarchyElements.Knowledge]!!.id
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!.id, dictionary[HierarchyElements.Ability]!!.id
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!.id, dictionary[HierarchyElements.Skill]!!.id
            ),
        )

        @JvmStatic
        @BeforeAll
        fun `fill dictionary`() {
            transaction {
                addLogger(StdOutSqlLogger)
                CQCElementDictionaryDAO.multiInsert(dictionary.values)
            }
        }

        @JvmStatic
        @AfterAll
        fun `clear dictionary`() {
            transaction {
                addLogger(StdOutSqlLogger)
                dictionary.values.forEach { CQCElementDictionaryDAO.deleteById(it.id) }
            }
        }
    }

    @Test
    override fun `select all without parameters`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val res = CQCElementHierarchyDAO.selectAll(orderBy = orderBy)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, hierarchy.size)
            assertEquals(hierarchy, res)

            rollback()
        }
    }

    @Test
    override fun `select all with limit`() {
        val limit = 3

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val res = CQCElementHierarchyDAO.selectAll(orderBy = orderBy, limit = limit)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(hierarchy.sortedBy { it.childId.toString() }.take(limit),
                res.sortedBy { it.childId.toString() })

            rollback()
        }
    }

    @Test
    override fun `select all with all search options`() {
        val limit = 2
        val offset = 1
        val order = "DESC"

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val res = CQCElementHierarchyDAO.selectAll(limit, offset.toLong(), orderBy, order)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(
                hierarchy.sortedBy { it.childId.toString() }.reversed().subList(offset, limit + offset).toSet(), res
            )

            rollback()
        }
    }

    @Test
    override fun `entity successfully created`() {
        transaction {
            addLogger(StdOutSqlLogger)

            val entity = hierarchy.first()
            val id = CQCElementHierarchyDAO.insert(entity)

            val res = CQCElementHierarchyDAO.selectById(entity.childId)

            assertTrue { id != null }
            assertEquals(entity.childId, id)
            assertTrue { res != null }
            assertEquals(entity, res)

            rollback()
        }
    }

    @Test
    override fun `entity updated successfully`() {
        transaction {
            addLogger(StdOutSqlLogger)

            val entity1 = hierarchy.first()
            val entity2 = hierarchy.last()

            val childId1 = CQCElementHierarchyDAO.insert(entity1) ?: throw SQLException("Entity not created")
            CQCElementHierarchyDAO.insert(entity2) ?: throw SQLException("Entity not created")

            val fromBD = CQCElementHierarchyDAO.selectById(entity1.childId)
            val forUpdate = CQCElementHierarchyEntity(parentId = entity2.parentId, childId = childId1)
            val updated = CQCElementHierarchyDAO.update(forUpdate)

            val res = CQCElementHierarchyDAO.selectById(forUpdate.childId)

            assertTrue { fromBD != null }
            assertEquals(updated, 1)
            assertTrue { res != null }
            assertEquals(res, forUpdate)

            rollback()
        }
    }

    @Test
    override fun `entity deleted successfully`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = hierarchy.first()

            val id = CQCElementHierarchyDAO.insert(entity) ?: throw SQLException("Entity not created")
            val deleted = CQCElementHierarchyDAO.deleteById(id)

            val res = CQCElementHierarchyDAO.selectById(id)

            assertEquals(deleted, 1)
            assertTrue { res == null }

            rollback()
        }
    }

    @Test
    fun `entity deletion at the top of hierarchy`() {
        val hierarchyTop = CQCElementHierarchyEntity(
            dictionary[HierarchyElements.Competence]!!.id, dictionary[HierarchyElements.Indicator]!!.id
        )
        val expectedRes = hierarchy.filterNot { it == hierarchyTop }.toSet()

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementHierarchyDAO.multiInsert(hierarchy)
            val deleted = CQCElementHierarchyDAO.deleteHierarchyLevel(hierarchyTop)

            val res = CQCElementHierarchyDAO.selectAll(orderBy = orderBy)

            assertEquals(deleted, 1)
            assertEquals(res, expectedRes)

            rollback()
        }
    }

    @Test
    fun `entity deletion at the bottom of hierarchy`() {
        val hierarchyBot = CQCElementHierarchyEntity(
            dictionary[HierarchyElements.Indicator]!!.id, dictionary[HierarchyElements.Skill]!!.id
        )
        val expectedRes = hierarchy.filterNot { it == hierarchyBot }.toSet()

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementHierarchyDAO.multiInsert(hierarchy)
            val deleted = CQCElementHierarchyDAO.deleteHierarchyLevel(hierarchyBot)

            val res = CQCElementHierarchyDAO.selectAll(orderBy = orderBy)

            assertEquals(deleted, 1)
            assertEquals(res, expectedRes)

            rollback()
        }
    }

    @Test
    fun `entity deletion at the middle of hierarchy`() {
        val newId = UUID.randomUUID()
        val newDictionaryElem = CQCElementDictionaryEntity(
            newId, "New element"
        )

        val hierarchyMiddle = CQCElementHierarchyEntity(
            parentId = newId, childId = dictionary[HierarchyElements.Indicator]!!.id
        )

        val localHierarchy = setOf(
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Competence]!!.id, hierarchyMiddle.parentId
            ),
            hierarchyMiddle,
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!.id, dictionary[HierarchyElements.Knowledge]!!.id
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!.id, dictionary[HierarchyElements.Ability]!!.id
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!.id, dictionary[HierarchyElements.Skill]!!.id
            ),
        )

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.insert(newDictionaryElem)
            CQCElementHierarchyDAO.multiInsert(localHierarchy)
            val deleted = CQCElementHierarchyDAO.deleteHierarchyLevel(hierarchyMiddle)

            val res = CQCElementHierarchyDAO.selectAll(orderBy = orderBy)

            assertEquals(deleted, 1)
            assertEquals(res, hierarchy)

            rollback()
        }
    }

    @Test
    fun `entity creating at the top of hierarchy`() {
        val newId = UUID.randomUUID()
        val newDictionaryElem = CQCElementDictionaryEntity(
            newId, "New element"
        )

        val hierarchyTop = CQCElementHierarchyEntity(
            parentId = newId, childId = dictionary[HierarchyElements.Competence]!!.id
        )

        val expectedHierarchy = hierarchy + hierarchyTop

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.insert(newDictionaryElem)
            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val newElemId = CQCElementHierarchyDAO.insert(hierarchyTop) ?: throw SQLException("Entity not created")
            val newElem = CQCElementDictionaryDAO.selectById(newElemId)

            val res = CQCElementHierarchyDAO.selectAll(orderBy = orderBy)

            assertTrue { newElem != null }
            assertEquals(res, expectedHierarchy)

            rollback()
        }
    }

    @Test
    fun `entity creating at the bot of hierarchy`() {
        val newId = UUID.randomUUID()
        val newDictionaryElem = CQCElementDictionaryEntity(
            newId, "New element"
        )

        val hierarchyBot = CQCElementHierarchyEntity(
            parentId = dictionary[HierarchyElements.Knowledge]!!.id,
            childId = newId
        )

        val expectedHierarchy = hierarchy + hierarchyBot

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.insert(newDictionaryElem)
            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val newElemId = CQCElementHierarchyDAO.insert(hierarchyBot) ?: throw SQLException("Entity not created")
            val newElem = CQCElementDictionaryDAO.selectById(newElemId)

            val res = CQCElementHierarchyDAO.selectAll(orderBy = orderBy)

            assertTrue { newElem != null }
            assertEquals(res, expectedHierarchy)

            rollback()
        }
    }

    @Test
    fun `entity creating at the middle of hierarchy`() {
        val newId = UUID.randomUUID()
        val newDictionaryElem = CQCElementDictionaryEntity(
            newId, "New element"
        )

        val hierarchyMiddle = CQCElementHierarchyEntity(
            parentId = newId,
            childId = dictionary[HierarchyElements.Indicator]!!.id
        )

        val expectedHierarchy = setOf(
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Competence]!!.id, newId
            ),
            hierarchyMiddle,
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!.id, dictionary[HierarchyElements.Knowledge]!!.id
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!.id, dictionary[HierarchyElements.Ability]!!.id
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!.id, dictionary[HierarchyElements.Skill]!!.id
            ),
        )

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.insert(newDictionaryElem)
            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val newElemId = CQCElementHierarchyDAO.insert(hierarchyMiddle) ?: throw SQLException("Entity not created")
            val newElem = CQCElementDictionaryDAO.selectById(newElemId)

            val res = CQCElementHierarchyDAO.selectAll(orderBy = orderBy)

            assertTrue { newElem != null }
            assertEquals(res, expectedHierarchy)

            rollback()
        }
    }
}