package database.model.dao.repository

import database.model.dao.entity.CQCElementDictionaryEntity
import database.model.dao.entity.CQCElementEntity
import database.model.dao.entity.CQCElementHierarchyEntity
import database.model.dao.entity.CourseEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CourseDAOTest : IDAOTest {
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

        private val competence1 = CQCElementEntity(
            UUID.randomUUID(),
            null,
            dictionary[HierarchyElements.Competence]!!,
            "Competence1"
        )

        private val indicator11 = CQCElementEntity(
            UUID.randomUUID(),
            competence1.id,
            dictionary[HierarchyElements.Indicator]!!,
            "Indicator11"
        )
        private val indicator12 = CQCElementEntity(
            UUID.randomUUID(),
            competence1.id,
            dictionary[HierarchyElements.Indicator]!!,
            "Indicator12"
        )
        private val indicator13 = CQCElementEntity(
            UUID.randomUUID(),
            competence1.id,
            dictionary[HierarchyElements.Indicator]!!,
            "Indicator13"
        )

        private val knowledge111 = CQCElementEntity(
            UUID.randomUUID(),
            indicator11.id,
            dictionary[HierarchyElements.Knowledge]!!,
            "Knowledge111"
        )
        private val knowledge112 = CQCElementEntity(
            UUID.randomUUID(),
            indicator11.id,
            dictionary[HierarchyElements.Knowledge]!!,
            "Knowledge112"
        )
        private val ability111 = CQCElementEntity(
            UUID.randomUUID(),
            indicator11.id,
            dictionary[HierarchyElements.Ability]!!,
            "Ability111"
        )
        private val skill111 = CQCElementEntity(
            UUID.randomUUID(),
            indicator11.id,
            dictionary[HierarchyElements.Skill]!!,
            "Skill111"
        )
        private val skill112 = CQCElementEntity(
            UUID.randomUUID(),
            indicator11.id,
            dictionary[HierarchyElements.Skill]!!,
            "Skill112"
        )


        private val competence2 = CQCElementEntity(
            UUID.randomUUID(),
            null,
            dictionary[HierarchyElements.Competence]!!,
            "Competence2"
        )

        private val indicator21 = CQCElementEntity(
            UUID.randomUUID(),
            competence2.id,
            dictionary[HierarchyElements.Indicator]!!,
            "Indicator21"
        )
        private val indicator22 = CQCElementEntity(
            UUID.randomUUID(),
            competence2.id,
            dictionary[HierarchyElements.Indicator]!!,
            "Indicator22"
        )

        private val knowledge221 = CQCElementEntity(
            UUID.randomUUID(),
            indicator22.id,
            dictionary[HierarchyElements.Knowledge]!!,
            "Knowledge221"
        )
        private val ability221 = CQCElementEntity(
            UUID.randomUUID(),
            indicator22.id,
            dictionary[HierarchyElements.Ability]!!,
            "Ability221"
        )
        private val ability222 = CQCElementEntity(
            UUID.randomUUID(),
            indicator22.id,
            dictionary[HierarchyElements.Ability]!!,
            "Ability222"
        )
        private val skill221 = CQCElementEntity(
            UUID.randomUUID(),
            indicator22.id,
            dictionary[HierarchyElements.Skill]!!,
            "Skill221"
        )
        private val skill222 = CQCElementEntity(
            UUID.randomUUID(),
            indicator22.id,
            dictionary[HierarchyElements.Skill]!!,
            "Skill222"
        )

        private val cqcElements = listOf(
            competence1, competence2,
            indicator11, indicator12, indicator13, indicator21, indicator22,
            knowledge111, knowledge112, knowledge221,
            ability111, ability221, ability222,
            skill111, skill112, skill221, skill222
        )

        private val defValues = setOf(
            CourseEntity(
                id = UUID.randomUUID(),
                name = "Курс1",
                inputLeafs = mapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence1),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator11),
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
                ),
                outputLeafs = mapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence2),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator21)
                )
            ),
//            CourseEntity(
//                id = UUID.randomUUID(),
//                name = "Курс2",
//                inputLeafs = mapOf(
//                    dictionary[HierarchyElements.Competence]!! to competence1,
//                    dictionary[HierarchyElements.Indicator]!! to indicator12
//                ),
//                outputLeafs = mapOf(
//                    dictionary[HierarchyElements.Competence]!! to competence2,
//                    dictionary[HierarchyElements.Indicator]!! to indicator22
//                )
//            ),
//            CourseEntity(
//                id = UUID.randomUUID(),
//                name = "Курс3",
//                inputLeafs = mapOf(
//                    dictionary[HierarchyElements.Competence]!! to competence2,
//                    dictionary[HierarchyElements.Indicator]!! to indicator22
//                ),
//                outputLeafs = mapOf(
//                    dictionary[HierarchyElements.Competence]!! to competence1,
//                    dictionary[HierarchyElements.Indicator]!! to indicator11
//                )
//            ),
//            CourseEntity(
//                id = UUID.randomUUID(),
//                name = "Курс4",
//                inputLeafs = mapOf(
//                    dictionary[HierarchyElements.Competence]!! to competence2,
//                    dictionary[HierarchyElements.Indicator]!! to indicator22
//                ),
//                outputLeafs = mapOf(
//                    dictionary[HierarchyElements.Competence]!! to competence1,
//                    dictionary[HierarchyElements.Indicator]!! to indicator13
//                )
//            ),
//            CourseEntity(
//                id = UUID.randomUUID(),
//                name = "Курс5",
//                inputLeafs = mapOf(
//                    dictionary[HierarchyElements.Competence]!! to competence1,
//                    dictionary[HierarchyElements.Indicator]!! to indicator13
//                ),
//                outputLeafs = mapOf(
//                    dictionary[HierarchyElements.Competence]!! to competence2,
//                    dictionary[HierarchyElements.Indicator]!! to indicator22
//                )
//            ),
        )

        @JvmStatic
        @BeforeAll
        fun `fill dictionary and hierarchy`() {
            transaction {
                addLogger(StdOutSqlLogger)
                CQCElementDictionaryDAO.multiInsert(dictionary.values)
                CQCElementHierarchyDAO.multiInsert(hierarchy)
                CQCElementDAO.multiInsert(cqcElements)
            }
        }

        @JvmStatic
        @AfterAll
        fun `clear dictionary`() {
            transaction {
                addLogger(StdOutSqlLogger)
                cqcElements.reversed().forEach { CQCElementDAO.deleteById(it.id) }
                hierarchy.forEach { CQCElementHierarchyDAO.deleteByPK(it.parentId, it.childId) }
                dictionary.values.forEach { CQCElementDictionaryDAO.deleteById(it.id) }
            }
        }
    }

    @Test
    @DisplayName("Successful insertion of one record")
    fun `entity successfully created`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = defValues.first()

            val id = CourseDAO.insert(entity)
            val res = CourseDAO.selectById(entity.id)
//           val res = CourseDAO.selectById(UUID.fromString("87db90d6-6d82-4a68-87c6-97bcf953d39a"))

//            println()
//            entity.inputLeafs.values.forEach { println(it.value) }
//            println()
//            res!!.inputLeafs.values.forEach { println(it.value) }
//            println()
//            CourseDAO.selectById(entity.id)!!.inputLeafs.values.forEach { println(it) }



            assertEquals(entity.id, id)
            assertTrue { res != null }
            assertEquals(res, entity)

        }
    }
}