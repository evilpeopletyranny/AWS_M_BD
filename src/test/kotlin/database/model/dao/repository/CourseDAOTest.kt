package database.model.dao.repository

import database.model.dao.entity.CQCElementDictionaryEntity
import database.model.dao.entity.CQCElementEntity
import database.model.dao.entity.CQCElementHierarchyEntity
import database.model.dao.entity.CourseEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.SQLException
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
                inputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence1),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator11),
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
                ),
                outputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence2),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator22),
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
                )
            ),
            CourseEntity(
                id = UUID.randomUUID(),
                name = "Курс2",
                inputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence1),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator11),
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
                ),
                outputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence2),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator21),
                )
            ),
            CourseEntity(
                id = UUID.randomUUID(),
                name = "Курс3",
                inputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence1),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator12),
                ),
                outputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence2),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator22),
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
                )
            ),
            CourseEntity(
                id = UUID.randomUUID(),
                name = "Курс4",
                inputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence1),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator13),
                ),
                outputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence2),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator21),
                )
            ),
            CourseEntity(
                id = UUID.randomUUID(),
                name = "Курс5",
                inputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence2),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator21),
                ),
                outputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence1),
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator12),
                )
            ),
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
                hierarchy.forEach { CQCElementHierarchyDAO.deleteByPK(it.parent.id, it.child.id) }
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

            assertEquals(entity.id, id)
            assertTrue { res != null }
            assertEquals(res, entity)

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion of one record - hierarchy violation: does not have a top level hierarchy")
    fun `element not created, because does not have a top level hierarchy`() {
        val invalidCourse = CourseEntity(
            id = UUID.randomUUID(),
            name = "Неверный курс",
            inputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Indicator]!! to setOf(indicator11),
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
            ),
            outputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Indicator]!! to setOf(indicator22),
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
            )
        )

        transaction {
            assertThrows<SQLException> {
                CourseDAO.insert(invalidCourse)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion of one record - hierarchy violation: skipping a hierarchy level")
    fun `element was not created because a hierarchy level was skipped`() {
        val invalidCourse = CourseEntity(
            id = UUID.randomUUID(),
            name = "Неверный курс",
            inputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Competence]!! to setOf(competence1),
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
            ),
            outputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Competence]!! to setOf(competence2),
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
            )
        )

        transaction {
            assertThrows<SQLException> {
                CourseDAO.insert(invalidCourse)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion of one record - hierarchy violation: association only with leafs")
    fun `element was not created because it only has an association with leafs`() {
        val invalidCourse = CourseEntity(
            id = UUID.randomUUID(),
            name = "Неверный курс",
            inputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
            ),
            outputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
            )
        )

        transaction {
            assertThrows<SQLException> {
                CourseDAO.insert(invalidCourse)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful insertion of multiple values")
    fun `successful creation of an entity set`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CourseDAO.multiInsert(defValues)
            val res = CourseDAO.selectAll()

            assertTrue { res.isNotEmpty() }
            assertEquals(defValues, res.sortedBy { it.name }.toSet())

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion multiple records - hierarchy violation: does not have a top level hierarchy")
    fun `entity set was not created, because does not have a top level hierarchy`() {
        val invalidCourse = CourseEntity(
            id = UUID.randomUUID(),
            name = "Неверный курс",
            inputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Indicator]!! to setOf(indicator11),
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
            ),
            outputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Indicator]!! to setOf(indicator22),
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
            )
        )
        val values = defValues + invalidCourse

        transaction {
            addLogger(StdOutSqlLogger)

            assertThrows<SQLException> {
                CourseDAO.multiInsert(values)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion multiple records - hierarchy violation: hierarchy violation: skipping a hierarchy level")
    fun `entity set was not created, because a hierarchy level was skipped`() {
        val invalidCourse = CourseEntity(
            id = UUID.randomUUID(),
            name = "Неверный курс",
            inputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Competence]!! to setOf(competence1),
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
            ),
            outputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Competence]!! to setOf(competence2),
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
            )
        )
        val values = defValues + invalidCourse

        transaction {
            addLogger(StdOutSqlLogger)

            assertThrows<SQLException> {
                CourseDAO.multiInsert(values)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion multiple records - hierarchy violation:  association only with leafs")
    fun `entity set was not created, because it only has an association with leafs`() {
        val invalidCourse = CourseEntity(
            id = UUID.randomUUID(),
            name = "Неверный курс",
            inputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
            ),
            outputLeafs = linkedMapOf(
                dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
            )
        )
        val values = defValues + invalidCourse

        transaction {
            addLogger(StdOutSqlLogger)

            assertThrows<SQLException> {
                CourseDAO.multiInsert(values)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful select all elements without search parameters")
    fun `select all without parameters`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CourseDAO.multiInsert(defValues)

            val res = CourseDAO.selectAll()

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, defValues.size)
            assertEquals(defValues, res)

            rollback()
        }
    }

    @Test
    @DisplayName("Successfully select a limited number of elements")
    fun `select all with limit`() {
        val limit = 3

        transaction {
            addLogger(StdOutSqlLogger)
            CourseDAO.multiInsert(defValues)

            val res = CourseDAO.selectAll(limit = limit)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(
                defValues.sortedBy { it.id.toString() }.take(limit).toSet(),
                res
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
            CourseDAO.multiInsert(defValues)

            val res = CourseDAO.selectAll(limit, offset.toLong(), orderBy, order)

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
    @DisplayName("Successfully select of one record")
    fun `select by id`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = defValues.first()
            val resId = CourseDAO.insert(entity)
            val res = resId?.let { CourseDAO.selectById(it) }

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

            CourseDAO.multiInsert(defValues)
            val res = CourseDAO.selectById(UUID.randomUUID())

            assertTrue { res == null }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful update of course parameter")
    fun `successful course parameter update`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CourseDAO.multiInsert(defValues)
            val fromBD = CourseDAO.selectById(defValues.last().id) ?: throw SQLException("Entity not created")

            val forUpdate = CourseEntity(
                id = fromBD.id,
                name = "Курс с новым именем",
                inputLeafs = fromBD.inputLeafs,
                outputLeafs = fromBD.outputLeafs
            )

            val updated = CourseDAO.update(forUpdate)
            val res = CourseDAO.selectById(forUpdate.id)

            assertTrue { res != null }
            assertTrue { updated == 1 }
            assertEquals(forUpdate, res)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful update of course links")
    fun `successful update of course links`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CourseDAO.multiInsert(defValues)
            val fromBD = CourseDAO.selectById(defValues.last().id) ?: throw SQLException("Entity not created")

            val forUpdate = CourseEntity(
                id = fromBD.id,
                name = "Курс с новым именем",
                inputLeafs = fromBD.outputLeafs,
                outputLeafs = fromBD.inputLeafs
            )

            val updated = CourseDAO.update(forUpdate)
            val res = CourseDAO.selectById(forUpdate.id)

            assertTrue { res != null }
            assertTrue { updated == 1 }
            assertEquals(forUpdate, res)

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful update of record - hierarchy violation: does not have a top level hierarchy")
    fun `element not updated, because does not have a top level hierarchy`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CourseDAO.multiInsert(defValues)
            val fromBD = CourseDAO.selectById(defValues.last().id) ?: throw SQLException("Entity not created")

            val forUpdate = CourseEntity(
                id = fromBD.id,
                name = fromBD.name,
                inputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator11),
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
                ),
                outputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Indicator]!! to setOf(indicator22),
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
                )
            )

            assertThrows<SQLException> {
                CourseDAO.update(forUpdate)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful update of record - hierarchy violation: skipping a hierarchy level")
    fun `element was not updated because a hierarchy level was skipped`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CourseDAO.multiInsert(defValues)
            val fromBD = CourseDAO.selectById(defValues.last().id) ?: throw SQLException("Entity not created")

            val forUpdate = CourseEntity(
                id = fromBD.id,
                name = fromBD.name,
                inputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence1),
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
                ),
                outputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Competence]!! to setOf(competence2),
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
                )
            )

            assertThrows<SQLException> {
                CourseDAO.update(forUpdate)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful update of record - hierarchy violation: association only with leafs")
    fun `element was not updated because it only has an association with leafs`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CourseDAO.multiInsert(defValues)
            val fromBD = CourseDAO.selectById(defValues.last().id) ?: throw SQLException("Entity not created")

            val forUpdate = CourseEntity(
                id = fromBD.id,
                name = fromBD.name,
                inputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge111, knowledge112),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability111),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill111, skill112),
                ),
                outputLeafs = linkedMapOf(
                    dictionary[HierarchyElements.Knowledge]!! to setOf(knowledge221),
                    dictionary[HierarchyElements.Ability]!! to setOf(ability221, ability222),
                    dictionary[HierarchyElements.Skill]!! to setOf(skill221, skill222)
                )
            )

            assertThrows<SQLException> {
                CourseDAO.update(forUpdate)
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

            val id = CourseDAO.insert(entity)
            val fromBD = CourseDAO.selectById(entity.id)
            val deleted = fromBD?.let { CourseDAO.deleteById(it.id) }
            val res = fromBD?.let { CourseDAO.selectById(it.id) }

            assertEquals(entity.id, id)
            assertEquals(deleted, 1)
            assertTrue { res == null }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessfully deletion - record does not exist")
    fun `Unsuccessfully record deletion - record does not exist`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CourseDAO.multiInsert(defValues)
            val deleted = CourseDAO.deleteById(UUID.randomUUID())

            assertEquals(deleted, 0)

            rollback()
        }
    }
}