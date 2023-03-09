package database.model.dao.repo

import database.model.dao.entity.CQCElementHierarchyEntity
import database.model.dao.entity.CQCElementHierarchyTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.sql.SQLDataException
import java.util.*

/**
 * Реализация DAO для взаимодействия с иерархией элементов ККХ: cqc_elem_hierarchy
 */
object CQCElementHierarchyDAO : ICQCElementHierarchyDAO {
    /**
     * Нахождение нужной колонки в таблице
     */
    private fun CQCElementHierarchyTable.column(columnName: String): Column<*> {
        return columns.find { it.name == columnName } ?: throw SQLDataException("Unknown column name: $columnName")
    }

    /**
     * Перевод результатов SQL запроса в отображение CQCElementHierarchyEntity
     */
    private fun ResultRow.toCQCElementHierarchyEntity(): CQCElementHierarchyEntity = CQCElementHierarchyEntity(
        childId = this[CQCElementHierarchyTable.childId],
        parentId = this[CQCElementHierarchyTable.parentId]
    )

    /**
     * Выбор всех записей таблицы.
     *
     * @param limit общее число записей выборки
     * @param offset отступ от первой записи
     * @param orderBy значение по которому необходимо провести сортировку
     * @param order порядок сортировки (ASC/DESC)
     * @return множество полученных элементов
     */
    override fun selectAll(
        limit: Int,
        offset: Long,
        orderBy: String,
        order: String
    ): Set<CQCElementHierarchyEntity> {
        return CQCElementHierarchyTable
            .selectAll()
            .limit(limit, offset)
            .orderBy(CQCElementHierarchyTable.column(orderBy) to SortOrder.valueOf(order))
            .map { it.toCQCElementHierarchyEntity() }
            .toSet()
    }

    /**
     * Выбор записи по ID
     *
     * @param id первичный ключ, по которому осуществляется поиск
     * @return nullable элемент (null если такой записи в таблице нет)
     */
    override fun selectById(id: UUID): CQCElementHierarchyEntity? {
        return CQCElementHierarchyTable.select {
            CQCElementHierarchyTable.childId eq id
        }.firstOrNull()?.toCQCElementHierarchyEntity()
    }

    /**
     * Вставка записи
     *
     * @param element элемент для вставки
     * @return nullable id вставленного элемента
     */
    override fun insert(element: CQCElementHierarchyEntity): UUID? {
        val insertedCount = CQCElementHierarchyTable.insert {
            it[childId] = element.childId
            it[parentId] = element.parentId
        }.insertedCount

        return if (insertedCount > 0) element.childId else null
    }

    /**
     * Вставка нескольких записей
     *
     * @param elements элементы для вставки
     */
    override fun multiInsert(elements: Collection<CQCElementHierarchyEntity>): List<ResultRow> {
        return CQCElementHierarchyTable.batchInsert(elements) {
            this[CQCElementHierarchyTable.childId] = it.childId
            this[CQCElementHierarchyTable.parentId] = it.parentId
        }
    }

    /**
     * Удаление записи по ID
     *
     * @param id первичный ключ по которому выполнится удаление
     * @return кол-во удаленных записей
     */
    override fun deleteById(id: UUID): Int {
        return CQCElementHierarchyTable.deleteWhere { childId eq id }
    }

    /**
     * Обновление уровня иерархии
     *
     * @param entity уровень, который необходимо обновить
     * @return кол-во обновленных записей
     */
    fun deleteHierarchyLevel(entity: CQCElementHierarchyEntity): Int {
        return CQCElementHierarchyTable.deleteWhere {
            Op.build { childId eq entity.childId and (parentId eq entity.parentId) }
        }
    }

    /**
     * Обновление записи
     *
     * @param element элемент, который необходимо обновить
     * @return кол-во обновленных записей
     */
    override fun update(element: CQCElementHierarchyEntity): Int {
        return CQCElementHierarchyTable.update({ CQCElementHierarchyTable.childId eq element.childId })
        { it[parentId] = element.parentId }
    }
}