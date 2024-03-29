package database.model.dao.repository

import database.model.dao.entity.CQCElementDictionaryEntity
import database.model.dao.entity.CQCElementHierarchyEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.SQLException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Interaction test with cqc_elem_hierarchy table")
class CQCElementHierarchyDAOTest : IDAOTest {

    companion object {
        enum class HierarchyElements {
            Competence, Indicator, Knowledge, Ability, Skill
        }

        private val dictionary = mapOf(
            HierarchyElements.Competence to CQCElementDictionaryEntity(
                UUID.randomUUID(), HierarchyElements.Competence.name, false
            ),
            HierarchyElements.Indicator to CQCElementDictionaryEntity(
                UUID.randomUUID(), HierarchyElements.Indicator.name, false
            ),
            HierarchyElements.Knowledge to CQCElementDictionaryEntity(
                UUID.randomUUID(), HierarchyElements.Knowledge.name, false
            ),
            HierarchyElements.Ability to CQCElementDictionaryEntity(
                UUID.randomUUID(), HierarchyElements.Ability.name, false
            ),
            HierarchyElements.Skill to CQCElementDictionaryEntity(
                UUID.randomUUID(), HierarchyElements.Skill.name, false
            ),
        )

        val hierarchy = setOf(
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Competence]!!, dictionary[HierarchyElements.Indicator]!!
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!, dictionary[HierarchyElements.Knowledge]!!
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!, dictionary[HierarchyElements.Ability]!!
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!, dictionary[HierarchyElements.Skill]!!
            ),
        )

        /**
         * Заполнение словаря перед выполнением тестов.
         * Иерархия содержит ссылку на типы элементов
         */
        @JvmStatic
        @BeforeAll
        fun `fill dictionary`() {
            transaction {
                addLogger(StdOutSqlLogger)
                CQCElementDictionaryDAO.multiInsert(dictionary.values)
            }
        }

        /**
         * Удаление словаря после выполнения тестов.
         */
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
    @DisplayName("Successful insertion of one record")
    fun `entity successfully created`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = hierarchy.first()

            val inserted = CQCElementHierarchyDAO.insert(entity)
            val res = CQCElementHierarchyDAO.selectByPK(entity.parent.id, entity.child.id)

            assertEquals(inserted, 1)
            assertEquals(res, entity)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful insertion of multiple values")
    fun `successful creation of an entity set`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val res = CQCElementHierarchyDAO.selectAll()

            assertTrue { res.isNotEmpty() }
            assertEquals(hierarchy, res)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful insertion of a top level of hierarchy")
    fun `top level hierarchy successfully created`() {
        val newDictionaryElem = CQCElementDictionaryEntity(
            UUID.randomUUID(), "New element", false
        )

        val hierarchyTop = CQCElementHierarchyEntity(
            parent = newDictionaryElem, child = dictionary[HierarchyElements.Competence]!!
        )

        val expectedHierarchy = hierarchy + hierarchyTop

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.insert(newDictionaryElem)
            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val inserted = CQCElementHierarchyDAO.insert(hierarchyTop)
            val res = CQCElementHierarchyDAO.selectAll()

            assertEquals(inserted, 1)
            assertEquals(res, expectedHierarchy)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful insertion of a mid level of hierarchy")
    fun `mid level hierarchy successfully created`() {
        val newDictionaryElem = CQCElementDictionaryEntity(
            UUID.randomUUID(), "New element", true
        )

        val hierarchyMiddle = CQCElementHierarchyEntity(
            parent = newDictionaryElem,
            child = dictionary[HierarchyElements.Indicator]!!
        )

        val expectedHierarchy = setOf(
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Competence]!!, newDictionaryElem
            ),
            hierarchyMiddle,
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!, dictionary[HierarchyElements.Knowledge]!!
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!, dictionary[HierarchyElements.Ability]!!
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!, dictionary[HierarchyElements.Skill]!!
            ),
        )

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.insert(newDictionaryElem)
            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val inserted = CQCElementHierarchyDAO.insert(hierarchyMiddle)
            val res = CQCElementHierarchyDAO.selectAll()

            assertEquals(inserted, 1)
            assertEquals(res, expectedHierarchy)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful insertion of a bot level of hierarchy")
    fun `entity creating at the bot of hierarchy`() {
        val newDictionaryElem = CQCElementDictionaryEntity(
            UUID.randomUUID(), "New element", false
        )

        val hierarchyBot = CQCElementHierarchyEntity(
            parent = dictionary[HierarchyElements.Knowledge]!!,
            child = newDictionaryElem
        )

        val expectedHierarchy = hierarchy + hierarchyBot

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.insert(newDictionaryElem)
            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val inserted = CQCElementHierarchyDAO.insert(hierarchyBot)
            val res = CQCElementHierarchyDAO.selectAll()

            assertEquals(inserted, 1)
            assertEquals(res, expectedHierarchy)

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion of one record - such record exists")
    fun `element not created because such element exists`() {
        transaction {
            addLogger(StdOutSqlLogger)

            val entity = hierarchy.first()
            CQCElementHierarchyDAO.insert(entity)

            assertThrows<SQLException> {
                CQCElementHierarchyDAO.insert(entity)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion of multi record - such record exists")
    fun `entity set was not created, because such name exist`() {
        transaction {
            val entity = hierarchy.first()
            CQCElementHierarchyDAO.insert(entity)

            assertThrows<SQLException> {
                CQCElementHierarchyDAO.multiInsert(hierarchy)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful select all elements without search parameters")
    fun `successful select all without parameters`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementHierarchyDAO.multiInsert(hierarchy)
            val res = CQCElementHierarchyDAO.selectAll()

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, hierarchy.size)
            assertEquals(hierarchy, res)

            rollback()
        }
    }

    @Test
    @DisplayName("Successfully select a limited number of elements")
    fun `successful select limited number elements`() {
        val limit = 3

        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val res = CQCElementHierarchyDAO.selectAll(limit)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(
                hierarchy.take(limit).toSet(), res
            )

            rollback()
        }
    }

    @Test
    @DisplayName("Successfully select with all search options")
    fun `successful select all with all search options`() {
        val limit = 3

        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementHierarchyDAO.multiInsert(hierarchy)

            val res = CQCElementHierarchyDAO.selectAll(limit)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(
                hierarchy.take(limit).toSet(), res
            )

            rollback()
        }
    }

    @Test
    @DisplayName("Successfully select of one record")
    fun `select one record`() {
        transaction {
            addLogger(StdOutSqlLogger)

            val entity = hierarchy.first()
            CQCElementHierarchyDAO.multiInsert(hierarchy)
            val res = CQCElementHierarchyDAO.selectByPK(entity.parent.id, entity.child.id)

            assertTrue { res != null }
            assertEquals(res, entity)

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessfully select of one record - not exists")
    fun `unsuccessfully select by id`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementHierarchyDAO.multiInsert(hierarchy)
            val res = CQCElementHierarchyDAO.selectByPK(UUID.randomUUID(), UUID.randomUUID())

            assertTrue { res == null }

            rollback()
        }
    }

    @Test
    @DisplayName("Successfully deletion of the record")
     fun `successfully record deletion`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = hierarchy.first()

            CQCElementHierarchyDAO.insert(entity)
            val deleted = CQCElementHierarchyDAO.deleteByPK(entity.parent.id, entity.child.id)

            val res = CQCElementHierarchyDAO.selectByPK(entity.parent.id, entity.child.id)

            assertEquals(deleted, 1)
            assertTrue { res == null }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessfully deletion - record does not exists")
    fun `unsuccessfully record deletion`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementHierarchyDAO.multiInsert(hierarchy)
            val deleted = CQCElementHierarchyDAO.deleteByPK(UUID.randomUUID(), UUID.randomUUID())

            assertEquals(deleted, 0)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful deletion of a top level of hierarchy")
    fun `top level hierarchy successfully deleted`() {
        val hierarchyTop = CQCElementHierarchyEntity(
            dictionary[HierarchyElements.Competence]!!, dictionary[HierarchyElements.Indicator]!!
        )
        val expectedRes = hierarchy.filterNot { it == hierarchyTop }.toSet()

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementHierarchyDAO.multiInsert(hierarchy)
            val deleted = CQCElementHierarchyDAO.deleteByPK(hierarchyTop.parent.id, hierarchyTop.child.id)

            val res = CQCElementHierarchyDAO.selectAll()

            assertEquals(deleted, 1)
            assertEquals(res, expectedRes)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful deletion of a mid level of hierarchy")
    fun `mid level hierarchy successfully deleted`() {
        val newDictionaryElem = CQCElementDictionaryEntity(
            UUID.randomUUID(), "New element", false
        )

        val hierarchyMiddle = CQCElementHierarchyEntity(
            parent = newDictionaryElem, child = dictionary[HierarchyElements.Indicator]!!
        )

        val localHierarchy = setOf(
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Competence]!!, hierarchyMiddle.parent
            ),
            hierarchyMiddle,
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!, dictionary[HierarchyElements.Knowledge]!!
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!, dictionary[HierarchyElements.Ability]!!
            ),
            CQCElementHierarchyEntity(
                dictionary[HierarchyElements.Indicator]!!, dictionary[HierarchyElements.Skill]!!
            ),
        )

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDictionaryDAO.insert(newDictionaryElem)
            CQCElementHierarchyDAO.multiInsert(localHierarchy)
            val deleted = CQCElementHierarchyDAO.deleteByPK(hierarchyMiddle.parent.id, hierarchyMiddle.child.id)

            val res = CQCElementHierarchyDAO.selectAll()

            assertEquals(deleted, 1)
            assertEquals(res, hierarchy)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful deletion of a bot level of hierarchy")
    fun `bot level hierarchy successfully deleted`() {
        val hierarchyBot = CQCElementHierarchyEntity(
            dictionary[HierarchyElements.Indicator]!!, dictionary[HierarchyElements.Skill]!!
        )
        val expectedRes = hierarchy.filterNot { it == hierarchyBot }.toSet()

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementHierarchyDAO.multiInsert(hierarchy)
            val deleted = CQCElementHierarchyDAO.deleteByPK(hierarchyBot.parent.id, hierarchyBot.child.id)

            val res = CQCElementHierarchyDAO.selectAll()

            assertEquals(deleted, 1)
            assertEquals(res, expectedRes)

            rollback()
        }
    }
}