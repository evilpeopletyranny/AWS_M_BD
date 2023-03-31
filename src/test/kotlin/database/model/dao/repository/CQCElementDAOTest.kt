package database.model.dao.repository

import database.model.dao.entity.CQCElementDictionaryEntity
import database.model.dao.entity.CQCElementEntity
import database.model.dao.entity.CQCElementHierarchyEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import java.sql.SQLException
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CQCElementDAOTest : IDAOTest {
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

        private val competence = CQCElementEntity(
            UUID.randomUUID(),
            null,
            dictionary[HierarchyElements.Competence]!!,
            "Competence1"
        )
        private val indicator = CQCElementEntity(
            UUID.randomUUID(),
            competence.id,
            dictionary[HierarchyElements.Indicator]!!,
            "Indicator1"
        )
        private val knowledge = CQCElementEntity(
            UUID.randomUUID(),
            indicator.id,
            dictionary[HierarchyElements.Knowledge]!!,
            "Knowledge1"
        )
        private val ability = CQCElementEntity(
            UUID.randomUUID(),
            indicator.id,
            dictionary[HierarchyElements.Ability]!!,
            "Ability1"
        )
        private val skill = CQCElementEntity(
            UUID.randomUUID(),
            indicator.id,
            dictionary[HierarchyElements.Skill]!!,
            "Skill1"
        )

        val defValues = setOf(
            competence,
            indicator,
            knowledge,
            ability,
            skill
        )

        @JvmStatic
        @BeforeAll
        fun `fill dictionary and hierarchy`() {
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
                hierarchy.forEach { CQCElementHierarchyDAO.deleteByPK(it.parent.id, it.child.id) }
            }
        }
    }

    @Test
    @DisplayName("Successful insertion of one record")
    fun `entity successfully created`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = defValues.first()

            val id = CQCElementDAO.insert(entity)
            val res = CQCElementDAO.selectById(entity.id)

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
            CQCElementDAO.multiInsert(defValues)

            val entity = CQCElementEntity(
                UUID.randomUUID(),
                indicator.id,
                dictionary[HierarchyElements.Skill]!!,
                knowledge.value
            )

            assertThrows<SQLException> {
                CQCElementDAO.insert(entity)
            }
        }
    }

    @Test
    @DisplayName("Successful insertion of multiple values")
    fun `successful creation of an entity set`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementDAO.multiInsert(defValues)
            val res = CQCElementDAO.selectAll()


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
            CQCElementDAO.insert(entity)

            assertThrows<SQLException> {
                CQCElementDAO.multiInsert(defValues)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful insertion with many child links")
    fun `successful creation of an entity set with many child links`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val competence1 = CQCElementEntity(
                UUID.randomUUID(),
                null,
                dictionary[HierarchyElements.Competence]!!,
                "Competence1"
            )

            val indicator11 = CQCElementEntity(
                UUID.randomUUID(),
                competence1.id,
                dictionary[HierarchyElements.Indicator]!!,
                "Indicator11"
            )
            val indicator12 = CQCElementEntity(
                UUID.randomUUID(),
                competence1.id,
                dictionary[HierarchyElements.Indicator]!!,
                "Indicator12"
            )
            val indicator13 = CQCElementEntity(
                UUID.randomUUID(),
                competence1.id,
                dictionary[HierarchyElements.Indicator]!!,
                "Indicator13"
            )

            val knowledge111 = CQCElementEntity(
                UUID.randomUUID(),
                indicator11.id,
                dictionary[HierarchyElements.Knowledge]!!,
                "Knowledge111"
            )
            val knowledge112 = CQCElementEntity(
                UUID.randomUUID(),
                indicator11.id,
                dictionary[HierarchyElements.Knowledge]!!,
                "Knowledge112"
            )
            val ability111 = CQCElementEntity(
                UUID.randomUUID(),
                indicator11.id,
                dictionary[HierarchyElements.Ability]!!,
                "Ability111"
            )
            val skill111 = CQCElementEntity(
                UUID.randomUUID(),
                indicator11.id,
                dictionary[HierarchyElements.Skill]!!,
                "Skill111"
            )
            val skill112 = CQCElementEntity(
                UUID.randomUUID(),
                indicator11.id,
                dictionary[HierarchyElements.Skill]!!,
                "Skill112"
            )


            val competence2 = CQCElementEntity(
                UUID.randomUUID(),
                null,
                dictionary[HierarchyElements.Competence]!!,
                "Competence2"
            )

            val indicator21 = CQCElementEntity(
                UUID.randomUUID(),
                competence2.id,
                dictionary[HierarchyElements.Indicator]!!,
                "Indicator21"
            )
            val indicator22 = CQCElementEntity(
                UUID.randomUUID(),
                competence2.id,
                dictionary[HierarchyElements.Indicator]!!,
                "Indicator22"
            )

            val knowledge221 = CQCElementEntity(
                UUID.randomUUID(),
                indicator22.id,
                dictionary[HierarchyElements.Knowledge]!!,
                "Knowledge221"
            )
            val ability221 = CQCElementEntity(
                UUID.randomUUID(),
                indicator22.id,
                dictionary[HierarchyElements.Ability]!!,
                "Ability221"
            )
            val ability222 = CQCElementEntity(
                UUID.randomUUID(),
                indicator22.id,
                dictionary[HierarchyElements.Ability]!!,
                "Ability222"
            )
            val skill221 = CQCElementEntity(
                UUID.randomUUID(),
                indicator22.id,
                dictionary[HierarchyElements.Skill]!!,
                "Skill221"
            )
            val skill222 = CQCElementEntity(
                UUID.randomUUID(),
                indicator22.id,
                dictionary[HierarchyElements.Skill]!!,
                "Skill222"
            )

            val values = setOf(
                competence1, competence2,
                indicator11, indicator12, indicator13, indicator21, indicator22,
                knowledge111, knowledge112, knowledge221,
                ability111, ability221, ability222,
                skill111, skill112, skill221, skill222
            )

            CQCElementDAO.multiInsert(values)
            val res = CQCElementDAO.selectAll()

            assertTrue { res.isNotEmpty() }
            assertEquals(values, res)

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion - non-top level element is missing a parent")
    fun `entity not created, hierarchy error (null parent of non-top-level element)`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val entity = CQCElementEntity(
                UUID.randomUUID(),
                null,
                dictionary[HierarchyElements.Indicator]!!,
                "New Indicator"
            )

            assertThrows<SQLException> {
                CQCElementDAO.insert(entity)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful insertion - invalid parent type")
    fun `entity not created, hierarchy error (invalid parent type)`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val entity = CQCElementEntity(
                UUID.randomUUID(),
                competence.id,
                dictionary[HierarchyElements.Skill]!!,
                "New Skill"
            )

            assertThrows<SQLException> {
                CQCElementDAO.insert(entity)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful insertion at the top of the hierarchy")
    fun `successful creation of the top level entity`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val entity = CQCElementEntity(
                UUID.randomUUID(),
                null,
                dictionary[HierarchyElements.Competence]!!,
                "New Competence"
            )

            val id = CQCElementDAO.insert(entity) ?: throw SQLException("Entity not created")
            val res = CQCElementDAO.selectById(id)

            assertEquals(res, entity)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful insertion at the mid of the hierarchy")
    fun `successful creation of the mid level entity`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val indicator = CQCElementEntity(
                UUID.randomUUID(),
                competence.id,
                dictionary[HierarchyElements.Indicator]!!,
                "New Indicator"
            )
            val skill = CQCElementEntity(
                UUID.randomUUID(),
                indicator.id,
                dictionary[HierarchyElements.Skill]!!,
                "New Skill"
            )

            CQCElementDAO.insert(indicator) ?: throw SQLException("Entity not created")
            CQCElementDAO.insert(skill) ?: throw SQLException("Entity not created")

            val indicatorFromBD = CQCElementDAO.selectById(indicator.id)
            val skillFromBD = CQCElementDAO.selectById(skill.id)

            assertEquals(indicator, indicatorFromBD)
            assertEquals(skill, skillFromBD)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful insertion at the bot of the hierarchy")
    fun `successful creation of the bot level entity`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val knowledge = CQCElementEntity(
                UUID.randomUUID(),
                indicator.id,
                dictionary[HierarchyElements.Knowledge]!!,
                "New Knowledge"
            )
            val ability = CQCElementEntity(
                UUID.randomUUID(),
                indicator.id,
                dictionary[HierarchyElements.Ability]!!,
                "New Ability"
            )
            val skill = CQCElementEntity(
                UUID.randomUUID(),
                indicator.id,
                dictionary[HierarchyElements.Skill]!!,
                "New Skill"
            )

            CQCElementDAO.insert(knowledge) ?: throw SQLException("Entity not created")
            CQCElementDAO.insert(ability) ?: throw SQLException("Entity not created")
            CQCElementDAO.insert(skill) ?: throw SQLException("Entity not created")

            val knowledgeFromBD = CQCElementDAO.selectById(knowledge.id)
            val abilityFromBD = CQCElementDAO.selectById(ability.id)
            val skillFromBD = CQCElementDAO.selectById(skill.id)

            assertEquals(knowledge, knowledgeFromBD)
            assertEquals(ability, abilityFromBD)
            assertEquals(skill, skillFromBD)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful select all elements without search parameters")
    fun `select all without parameters`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)

            val res = CQCElementDAO.selectAll()

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
            CQCElementDAO.multiInsert(defValues)

            val res = CQCElementDAO.selectAll(limit = limit)

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
        val orderBy = "type_id"
        val order = "DESC"

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)

            val res = CQCElementDAO.selectAll(limit, offset.toLong(), orderBy, order)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(
                defValues.sortedBy { it.type.toString() }.reversed().subList(offset, limit + offset).toSet(),
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
            val resId = CQCElementDAO.insert(entity)
            val res = resId?.let { CQCElementDAO.selectById(it) }

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

            CQCElementDAO.multiInsert(defValues)
            val res = CQCElementDAO.selectById(UUID.randomUUID())

            assertTrue { res == null }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful update of one entity")
    fun `successfully entity update`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val fromBD = CQCElementDAO.selectById(defValues.last().id) ?: throw SQLException("Entity not created")

            val forUpdate = CQCElementEntity(
                id = fromBD.id,
                parentId = null,
                type = dictionary[HierarchyElements.Competence]!!,
                value = "New competence"
            )

            val updated = CQCElementDAO.update(forUpdate)
            val res = CQCElementDAO.selectById(forUpdate.id)

            assertTrue { res != null }
            assertTrue { updated == 1 }
            assertEquals(forUpdate, res)

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful update of one entity - such name exists")
    fun `entity not updated, because such name exist`() {
        val newName = "New Ability"

        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val fromBD =
                CQCElementDAO.selectById(ability.id) ?: throw SQLException("Entity not created")
            val entity = CQCElementEntity(
                UUID.randomUUID(),
                fromBD.parentId,
                fromBD.type,
                newName
            )
            val entityId = CQCElementDAO.insert(entity)

            val entityUpdate = CQCElementEntity(
                fromBD.id,
                fromBD.parentId,
                fromBD.type,
                newName
            )

            assertTrue(entityId != null)
            assertThrows<SQLException> {
                CQCElementDAO.update(entityUpdate)
            }
        }
    }

    @Test
    @DisplayName("Unsuccessful update - non-top level element is missing a parent")
    fun `entity not updated, hierarchy error (null parent of non-top-level element)`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val indicator =
                CQCElementDAO.selectById(indicator.id) ?: throw SQLException("Entity not created")
            val indicatorUpdate = CQCElementEntity(
                indicator.id,
                null,
                indicator.type,
                indicator.value
            )

            assertThrows<SQLException> {
                CQCElementDAO.update(indicatorUpdate)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful update - hierarchy error (violation - wrong parent)")
    fun `entity not updated, hierarchy error (violation - wrong parent)`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val indicator =
                CQCElementDAO.selectById(indicator.id)
                    ?: throw SQLException("Entity not created")
            val indicatorUpdate = CQCElementEntity(
                indicator.id,
                skill.id,
                indicator.type,
                indicator.value
            )

            assertThrows<SQLException> {
                CQCElementDAO.update(indicatorUpdate)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessful update - hierarchy error (violation - wrong type)")
    fun `entity not updated, hierarchy error (violation - wrong type)`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val indicator =
                CQCElementDAO.selectById(indicator.id) ?: throw SQLException("Entity not created")
            val indicatorUpdate = CQCElementEntity(
                indicator.id,
                indicator.parentId,
                dictionary[HierarchyElements.Skill]!!,
                indicator.value
            )

            assertThrows<SQLException> {
                CQCElementDAO.update(indicatorUpdate)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful update at the top of the hierarchy")
    fun `successful update of the top level entity`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)

            val fromBd = CQCElementDAO.selectById(competence.id) ?: throw SQLException("Entity not created")
            val competenceUpdate = CQCElementEntity(
                fromBd.id,
                fromBd.parentId,
                fromBd.type,
                "New competence"
            )

            val updated = CQCElementDAO.update(competenceUpdate)
            val res = CQCElementDAO.selectById(competence.id)

            assertEquals(updated, 1)
            assertEquals(res, competenceUpdate)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful update at the mid of the hierarchy")
    fun `successful update of the mid level entity`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)

            val fromBd = CQCElementDAO.selectById(indicator.id) ?: throw SQLException("Entity not created")
            val competenceUpdate = CQCElementEntity(
                fromBd.id,
                fromBd.parentId,
                fromBd.type,
                "New indicator"
            )

            val updated = CQCElementDAO.update(competenceUpdate)
            val res = CQCElementDAO.selectById(indicator.id)

            assertEquals(updated, 1)
            assertEquals(res, competenceUpdate)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful update at the bot of the hierarchy")
    fun `successful update of the bot level entity`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)

            val fromBd = CQCElementDAO.selectById(knowledge.id) ?: throw SQLException("Entity not created")
            val competenceUpdate = CQCElementEntity(
                fromBd.id,
                fromBd.parentId,
                fromBd.type,
                "New knowledge"
            )

            val updated = CQCElementDAO.update(competenceUpdate)
            val res = CQCElementDAO.selectById(knowledge.id)

            assertEquals(updated, 1)
            assertEquals(res, competenceUpdate)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful deletion of the record")
    fun `entity deleted successfully`() {
        transaction {
            addLogger(StdOutSqlLogger)
            val entity = defValues.first()

            val id = CQCElementDAO.insert(entity)
            val fromBD = CQCElementDAO.selectById(entity.id)
            val deleted = fromBD?.let { CQCElementDAO.deleteById(it.id) }
            val res = fromBD?.let { CQCElementDAO.selectById(it.id) }

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

            CQCElementDAO.multiInsert(defValues)
            val deleted = CQCElementDAO.deleteById(UUID.randomUUID())

            assertEquals(deleted, 0)

            rollback()
        }
    }

    @Test
    @DisplayName("Unsuccessfully deletion - having a child")
    fun `Unsuccessfully record deletion - having a child`() {
        transaction {
            addLogger(StdOutSqlLogger)

            CQCElementDAO.multiInsert(defValues)

            assertThrows<SQLException> {
                CQCElementDAO.deleteById(competence.id)
            }

            rollback()
        }
    }

    @Test
    @DisplayName("Successful delete at the bot of the hierarchy")
    fun `successful delete of the bot level entity`() {
        val expectedRes = defValues - knowledge - ability - skill
        var deleted = 0

        transaction {
            CQCElementDAO.multiInsert(defValues)

            deleted += CQCElementDAO.deleteById(knowledge.id)
            deleted += CQCElementDAO.deleteById(ability.id)
            deleted += CQCElementDAO.deleteById(skill.id)

            val res = CQCElementDAO.selectAll()

            assertEquals(deleted, 3)
            assertEquals(res, expectedRes)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful delete at the mid of the hierarchy")
    fun `successful delete of the mid level entity`() {
        val expectedRes = defValues.filter { it == competence }.toSet()
        var deleted = 0

        transaction {
            CQCElementDAO.multiInsert(defValues)

            deleted += CQCElementDAO.deleteById(knowledge.id)
            deleted += CQCElementDAO.deleteById(ability.id)
            deleted += CQCElementDAO.deleteById(skill.id)
            deleted += CQCElementDAO.deleteById(indicator.id)

            val res = CQCElementDAO.selectAll()

            assertEquals(deleted, 4)
            assertEquals(res, expectedRes)

            rollback()
        }
    }

    @Test
    @DisplayName("Successful delete at the mid of the hierarchy")
    fun `successful delete of the top level entity`() {
        val expectedRes = emptySet<CQCElementEntity>()
        var deleted = 0

        transaction {
            CQCElementDAO.multiInsert(defValues)

            deleted += CQCElementDAO.deleteById(knowledge.id)
            deleted += CQCElementDAO.deleteById(ability.id)
            deleted += CQCElementDAO.deleteById(skill.id)
            deleted += CQCElementDAO.deleteById(indicator.id)
            deleted += CQCElementDAO.deleteById(competence.id)

            val res = CQCElementDAO.selectAll()

            assertEquals(deleted, 5)
            assertEquals(res, expectedRes)

            rollback()
        }
    }
}