package database.model.dao.repository

import database.model.dao.entity.CQCElementHierarchyEntity
import database.model.dao.table.CQCElementHierarchyTable
import org.jetbrains.exposed.sql.*
import java.util.*

/**
 * Реализация DAO для взаимодействия с иерархией элементов ККХ: cqc_elem_hierarchy
 */
object CQCElementHierarchyDAO : ICQCElementHierarchyDAO {
    /**
     * Перевод результатов SQL запроса в отображение CQCElementHierarchyEntity
     */
    private fun ResultRow.toCQCElementHierarchyEntity(): CQCElementHierarchyEntity = CQCElementHierarchyEntity(
        childId = this[CQCElementHierarchyTable.childId],
        parentId = this[CQCElementHierarchyTable.parentId]
    )

    /**
     * Выбор записи по Primary Key
     *
     * @param firstPart parent_id
     * @param secondPart child_id
     * @return nullable элемент (null если такой записи в таблице нет)
     */
    override fun selectByPK(firstPart: UUID, secondPart: UUID): CQCElementHierarchyEntity? {
        return CQCElementHierarchyTable.select {
            Op.build { CQCElementHierarchyTable.parentId eq firstPart and (CQCElementHierarchyTable.childId eq secondPart) }
        }.firstOrNull()?.toCQCElementHierarchyEntity()
    }

    /**
     * Удаление записи по Primary Key
     *
     * @param firstPart parent_id
     * @param secondPart child_id
     * @return кол-во удаленных записей
     */
    override fun deleteByPK(firstPart: UUID, secondPart: UUID): Int {
        return CQCElementHierarchyTable.deleteWhere {
            Op.build { parentId eq firstPart and (childId eq secondPart) }
        }
    }

    /**
     * Выбор всех записей таблицы.
     *
     * @param limit общее число записей выборки
     * @return множество полученных элементов
     */
    override fun selectAll(
        limit: Int,
    ): Set<CQCElementHierarchyEntity> {
        return CQCElementHierarchyTable
            .selectAll()
            .limit(limit)
            .map { it.toCQCElementHierarchyEntity() }
            .toSet()
    }

    /**
     * Вставка записи
     *
     * @param element элемент для вставки
     * @return nullable id вставленного элемента
     */
    override fun insert(element: CQCElementHierarchyEntity): Int {
        return CQCElementHierarchyTable.insert {
            it[childId] = element.childId
            it[parentId] = element.parentId
        }.insertedCount
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
}