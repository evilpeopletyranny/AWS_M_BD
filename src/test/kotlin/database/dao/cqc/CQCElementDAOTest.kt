package database.dao.cqc

import database.dao.IDAOTest
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
import kotlin.test.assertTrue

class CQCElementDAOTest : IDAOTest {
    private val orderBy = "id"

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
     * Выборка записи по id
     */
    @Test
    override fun `select by id`() {
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

    /**
     * Выборка всех записей без параметров поиска
     */
    @Test
    override fun `select all without parameters`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)

            val res = CQCElementDAO.selectAll(orderBy = orderBy)

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
            CQCElementDAO.multiInsert(defValues)

            val res = CQCElementDAO.selectAll(orderBy = orderBy, limit = limit)

            assertTrue { res.isNotEmpty() }
            assertEquals(res.size, limit)
            assertEquals(
                defValues.sortedBy { it.id.toString() }.take(limit),
                res.sortedBy { it.id.toString() })

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

    /**
     * Успешное создание записи в таблице
     */
    @Test
    override fun `entity successfully created`() {
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
    fun `entity not created, because such name exist`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)

            val entity = CQCElementEntity(
                UUID.randomUUID(),
                UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"),    //def indicator id
                dictionary[HierarchyElements.Skill]!!,
                value = "Knowledge1"   //def knowledge name
            )

            assertThrows<SQLException> {
                CQCElementDAO.insert(entity)
            }
        }
    }

    /**
     * Успешное обновление записи в таблице
     */
    @Test
    override fun `entity updated successfully`() {
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

    /**
     * Ошибка при обновлении записи - нарушение уникальности имен
     */
    @Test
    fun `entity not updated, because such name exist`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val fromBD =
                CQCElementDAO.selectById(UUID.fromString("37bc41f9-866f-4dee-9e9e-925590a74b6d"))  //def ability id
                    ?: throw SQLException("Entity not created")

            val entityUpdate = CQCElementEntity(
                fromBD.id,
                fromBD.parentId,
                fromBD.type,
                value = "Competence1"   //def competence name
            )

            assertThrows<SQLException> {
                CQCElementDAO.update(entityUpdate)
            }
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

    /**
     * Проверка создания элемента, находящегося на высшем уровне иерархии (Компетенция - не имеет родителя)
     */
    @Test
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

    /**
     * Проверка создание элемента, находящихся на среднем уровне иерархии (Имеют родителя и потомка)
     */
    @Test
    fun `successful creation of the mid level entity`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val indicator = CQCElementEntity(
                UUID.randomUUID(),
                UUID.fromString("d039f326-184c-4f0e-b4f4-589098a56ff3"),    //def competence id
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

    /**
     * Проверка создание элемента, находящегося на нижнем уровне иерархии (имеет только родителя)
     */
    @Test
    fun `successful creation of the bot level entity`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val knowledge = CQCElementEntity(
                UUID.randomUUID(),
                UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"),
                dictionary[HierarchyElements.Knowledge]!!,
                "New Knowledge"
            )
            val ability = CQCElementEntity(
                UUID.randomUUID(),
                UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"),
                dictionary[HierarchyElements.Ability]!!,
                "New Ability"
            )
            val skill = CQCElementEntity(
                UUID.randomUUID(),
                UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"),
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

    /**
     * Ошибка - при создании элемента не высшего уровня отсутствует родитель
     */
    @Test
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

    /**
     * Ошибка при создании элемента и нарушении иерархии.
     * Изменен тип элемента не подходит типу родителя
     */
    @Test
    fun `entity not created, hierarchy error (violation - wrong type)`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val entity = CQCElementEntity(
                UUID.randomUUID(),
                UUID.fromString("d039f326-184c-4f0e-b4f4-589098a56ff3"),    //def competence id
                dictionary[HierarchyElements.Skill]!!,
                "New Skill"
            )

            assertThrows<SQLException> {
                CQCElementDAO.insert(entity)
            }

            rollback()
        }
    }


    /**
     * Ошибка - при обновлении элемента не высшего уровня отсутствует родитель
     */
    @Test
    fun `entity not updated, hierarchy error (null parent of non-top-level element)`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val indicator =
                CQCElementDAO.selectById(UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"))   //def indicator id
                    ?: throw SQLException("Entity not created")
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

    /**
     * Ошибка при обновлении элемента и нарушении иерархии.
     * Изменен тип элемента
     */
    @Test
    fun `entity not updated, hierarchy error (violation - wrong type)`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val indicator =
                CQCElementDAO.selectById(UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"))   //def indicator id
                    ?: throw SQLException("Entity not created")
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

    /**
     * Ошибка при обновлении элемента и нарушении иерархии.
     * Изменен родитель элемента
     */
    @Test
    fun `entity not updated, hierarchy error (violation - wrong parent)`() {
        transaction {
            addLogger(StdOutSqlLogger)
            CQCElementDAO.multiInsert(defValues)
            val indicator =
                CQCElementDAO.selectById(UUID.fromString("ef8f31ff-c560-439c-9264-5403dcca8b1b"))   //def indicator id
                    ?: throw SQLException("Entity not created")
            val indicatorUpdate = CQCElementEntity(
                indicator.id,
                UUID.fromString("b0efbc17-44a7-4f22-bcd7-b7ddfb34721b"),    //default skill id
                indicator.type,
                indicator.value
            )

            assertThrows<SQLException> {
                CQCElementDAO.update(indicatorUpdate)
            }

            rollback()
        }
    }
}