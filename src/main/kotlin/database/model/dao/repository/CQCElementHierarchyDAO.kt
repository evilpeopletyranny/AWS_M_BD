package database.model.dao.repository

import database.model.dao.entity.CQCElementDictionaryEntity
import database.model.dao.entity.CQCElementHierarchyEntity
import database.model.dao.table.CQCElementDictionaryTable
import database.model.dao.table.CQCElementHierarchyTable
import org.jetbrains.exposed.sql.*
import java.util.*

/**
 * Реализация DAO для взаимодействия с иерархией элементов ККХ: cqc_elem_hierarchy
 */
object CQCElementHierarchyDAO : ICQCElementHierarchyDAO {
    private val dict1 = CQCElementDictionaryTable.alias("dict1")
    private val dict2 = CQCElementDictionaryTable.alias("dict2")

    /**
     * Перевод результатов SQL запроса в отображение CQCElementHierarchyEntity
     */
    private fun ResultRow.toCQCElementHierarchyEntity(): CQCElementHierarchyEntity = CQCElementHierarchyEntity(
        parent = CQCElementDictionaryEntity(
            id = this[dict1[CQCElementDictionaryTable.id]].value,
            name = this[dict1[CQCElementDictionaryTable.name]],
            isDeleted = this[dict1[CQCElementDictionaryTable.isDeleted]]
        ),
        child = CQCElementDictionaryEntity(
            id = this[dict2[CQCElementDictionaryTable.id]].value,
            name = this[dict2[CQCElementDictionaryTable.name]],
            isDeleted = this[dict2[CQCElementDictionaryTable.isDeleted]]
        )
    )

    /**
     * Выбор записи по Primary Key
     *
     * @param firstPart parent
     * @param secondPart child
     * @return nullable элемент (null если такой записи в таблице нет)
     */
    override fun selectByPK(firstPart: UUID, secondPart: UUID): CQCElementHierarchyEntity? {
        return CQCElementHierarchyTable.leftJoin(
            dict1, { parent }, { dict1[CQCElementDictionaryTable.id] }
        ).leftJoin(
            dict2, { CQCElementHierarchyTable.child }, { dict2[CQCElementDictionaryTable.id] }
        ).slice(
            dict1[CQCElementDictionaryTable.id],
            dict1[CQCElementDictionaryTable.name],
            dict1[CQCElementDictionaryTable.isDeleted],
            dict2[CQCElementDictionaryTable.id],
            dict2[CQCElementDictionaryTable.name],
            dict2[CQCElementDictionaryTable.isDeleted]
        ).select {
            Op.build { CQCElementHierarchyTable.parent eq firstPart and (CQCElementHierarchyTable.child eq secondPart) }
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
            Op.build { parent eq firstPart and (child eq secondPart) }
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
        return CQCElementHierarchyTable.leftJoin(
            dict1, { parent }, { dict1[CQCElementDictionaryTable.id] }
        ).leftJoin(
            dict2, { CQCElementHierarchyTable.child }, { dict2[CQCElementDictionaryTable.id] }
        ).slice(
            dict1[CQCElementDictionaryTable.id],
            dict1[CQCElementDictionaryTable.name],
            dict1[CQCElementDictionaryTable.isDeleted],
            dict2[CQCElementDictionaryTable.id],
            dict2[CQCElementDictionaryTable.name],
            dict2[CQCElementDictionaryTable.isDeleted]
        ).selectAll()
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
            it[child] = element.child.id
            it[parent] = element.parent.id
        }.insertedCount
    }

    /**
     * Вставка нескольких записей
     *
     * @param elements элементы для вставки
     */
    override fun multiInsert(elements: Collection<CQCElementHierarchyEntity>): List<ResultRow> {
        return CQCElementHierarchyTable.batchInsert(elements) {
            this[CQCElementHierarchyTable.child] = it.child.id
            this[CQCElementHierarchyTable.parent] = it.parent.id
        }
    }

    fun getHierarchy(): LinkedHashSet<CQCElementDictionaryEntity> {
        val entities = selectAll()
        val parents = entities.map { it.parent }
        val children = entities.map { it.child }

        println(parents)
        println(children)

        val res: LinkedHashSet<CQCElementDictionaryEntity> = LinkedHashSet()
        res.add(
            parents.minus(
                children.toHashSet()).first())

        entities.forEach { _ ->
            val last = res.last()
            res.addAll(entities.filter { it.parent == last }.map { it.child })
        }

        return res
    }
}