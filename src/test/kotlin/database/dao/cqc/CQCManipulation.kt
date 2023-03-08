package database.dao.cqc

import database.DatabaseFactory
import database.entity.cqc.CQCElementDictionaryEntity
import database.entity.cqc.CQCElementEntity
import database.entity.cqc.CQCElementHierarchyEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import java.util.*
import kotlin.test.assertEquals

class CQCManipulation {
    private val orderBy = "id"
    private val hierarchyOrderBy = "child_type_id"

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
            HierarchyElements.Ability to CQCElementDictionaryEntity(
                UUID.randomUUID(), HierarchyElements.Ability.name
            ),
            HierarchyElements.Skill to CQCElementDictionaryEntity(
                UUID.randomUUID(), HierarchyElements.Skill.name
            ),
        )

        private val hierarchy = setOf(
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

        val defValues = setOf(
            CQCElementEntity(
                UUID.fromString("d039f326-184c-4f0e-b4f4-589098a56ff3"),
                null,
                dictionary[HierarchyElements.Competence]!!,
                "Competence1"
            ),
            CQCElementEntity(
                UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"),
                UUID.fromString("d039f326-184c-4f0e-b4f4-589098a56ff3"),
                dictionary[HierarchyElements.Indicator]!!,
                "Indicator1"
            ),
            CQCElementEntity(
                UUID.fromString("be9734cd-200a-4408-9ef0-11cbdf8ee34f"),
                UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"),
                dictionary[HierarchyElements.Knowledge]!!,
                "Knowledge1"
            ),
            CQCElementEntity(
                UUID.fromString("37bc41f9-866f-4dee-9e9e-925590a74b6d"),
                UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"),
                dictionary[HierarchyElements.Ability]!!,
                "Ability1"
            ),
            CQCElementEntity(
                UUID.fromString("b0efbc17-44a7-4f22-bcd7-b7ddfb34721b"),
                UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"),
                dictionary[HierarchyElements.Skill]!!,
                "Skill1"
            )
        )

        @JvmStatic
        @BeforeAll
        fun `fill dictionary and hierarchy`() {
            DatabaseFactory.init(
                url = """jdbc:postgresql://localhost:5432/cqc_test?
                        reWriteBatchedInserts=true& +
                        rewriteBatchedStatements=true&
                        shouldReturnGeneratedValues=false"""
            )

            transaction {
                addLogger(StdOutSqlLogger)
                CQCElementDictionaryDAO.multiInsert(dictionary.values)
                CQCElementHierarchyDAO.multiInsert(hierarchy)
            }
        }

        @JvmStatic
        @AfterAll
        fun `clear dictionary`() {
            transaction {
                addLogger(StdOutSqlLogger)
                dictionary.values.forEach { CQCElementDictionaryDAO.deleteById(it.id) }
                hierarchy.forEach { CQCElementHierarchyDAO.deleteHierarchyLevel(it) }
            }
        }
    }

    /**
     * Удалять элементы с дочерними связями - невозможно
     */
    @Test
    fun `deleting an element with child elements is not possible`() {
        val type = dictionary[HierarchyElements.Indicator]

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val entity = defValues.find { it.type == type }!!


            val localDictionary = CQCElementDictionaryDAO.selectAll(orderBy = orderBy)
            val localHierarchy = CQCElementHierarchyDAO.selectAll(orderBy = hierarchyOrderBy)

            assertThrows<SQLException> {
                CQCElementDAO.deleteById(entity.id)
            }
            assertEquals(dictionary.values.toSet(), localDictionary)
            assertEquals(hierarchy, localHierarchy)
            rollback()
        }
    }

    /**
     * Удаление типа влияет на иерархию
     */
    @Test
    fun `deleting an cqc type affects the hierarchy`() {
        val type = dictionary[HierarchyElements.Indicator]!!

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)

            val deleted = CQCElementDictionaryDAO.deleteById(type.id)
            val localHierarchy = CQCElementHierarchyDAO.selectAll(orderBy = hierarchyOrderBy)

            println(localHierarchy)

            assertEquals(deleted, 1)

            rollback()
        }
    }
}