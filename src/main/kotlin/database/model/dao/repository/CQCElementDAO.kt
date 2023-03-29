package database.model.dao.repository

import database.model.dao.entity.CQCElementDictionaryEntity
import database.model.dao.table.CQCElementDictionaryTable
import database.model.dao.entity.CQCElementEntity
import database.model.dao.table.CQCElementTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.sql.SQLDataException
import java.util.*

/**
 * Реализация DAO для взаимодействия с таблицей ККХ: cqc_elem
 */
object CQCElementDAO : ICQCElementDAO {
    /**
     * Нахождение нужной колонки в таблице
     */
    private fun CQCElementTable.column(columnName: String): Column<*> {
        return columns.find { it.name == columnName } ?: throw SQLDataException("Unknown column name: $columnName")
    }

    /**
     * Перевод результатов SQL запроса в отображение CQCElementHierarchyEntity
     */
     fun ResultRow.toCQCElementEntity(): CQCElementEntity = CQCElementEntity(
        id = this[CQCElementTable.id].value,
        parentId = this[CQCElementTable.parentId]?.value,
        type = CQCElementDictionaryEntity(
            id = this[CQCElementDictionaryTable.id].value,
            name = this[CQCElementDictionaryTable.name],
            isDeleted = this[CQCElementDictionaryTable.isDeleted]
        ),
        value = this[CQCElementTable.value]
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
    ): Set<CQCElementEntity> {
        return CQCElementTable.join(
            CQCElementDictionaryTable,
            JoinType.LEFT,
            additionalConstraint = {
                CQCElementTable.type eq CQCElementDictionaryTable.id
            })
            .slice(
                CQCElementTable.id,
                CQCElementTable.parentId,
                CQCElementDictionaryTable.id,
                CQCElementDictionaryTable.name,
                CQCElementDictionaryTable.isDeleted,
                CQCElementTable.value
            )
            .selectAll()
            .limit(limit, offset)
            .orderBy(CQCElementTable.column(orderBy) to SortOrder.valueOf(order))
            .map { it.toCQCElementEntity() }
            .toSet()
    }

    /**
     * Выбор записи по ID
     *
     * @param id первичный ключ, по которому осуществляется поиск
     * @return nullable элемент (null если такой записи в таблице нет)
     */
    override fun selectById(id: UUID): CQCElementEntity? {
        return CQCElementTable.join(
            CQCElementDictionaryTable,
            JoinType.LEFT,
            additionalConstraint = {
                CQCElementTable.type eq CQCElementDictionaryTable.id
            })
            .slice(
                CQCElementTable.id,
                CQCElementTable.parentId,
                CQCElementDictionaryTable.id,
                CQCElementDictionaryTable.name,
                CQCElementDictionaryTable.isDeleted,
                CQCElementTable.value
            )
            .select {
                CQCElementTable.id eq id
            }.firstOrNull()?.toCQCElementEntity()
    }

    /**
     * Вставка записи
     *
     * @param element элемент для вставки
     * @return nullable id вставленного элемента
     */
    override fun insert(element: CQCElementEntity): UUID? {
        val insertedCount = CQCElementTable.insert {
            it[id] = element.id
            it[parentId] = element.parentId
            it[type] = element.type.id
            it[value] = element.value
        }.insertedCount

        return if (insertedCount > 0) element.id else null
    }

    /**
     * Вставка нескольких записей
     *
     * @param elements элементы для вставки
     */
    override fun multiInsert(elements: Collection<CQCElementEntity>): List<ResultRow> {
        return CQCElementTable.batchInsert(elements) {
            this[CQCElementTable.id] = it.id
            this[CQCElementTable.parentId] = it.parentId
            this[CQCElementTable.type] = it.type.id
            this[CQCElementTable.value] = it.value
        }
    }

    /**
     * Удаление записи по ID
     *
     * @param id первичный ключ по которому выполнится удаление
     * @return кол-во удаленных записей
     */
    override fun deleteById(id: UUID): Int {
        return CQCElementTable.deleteWhere { CQCElementTable.id eq id }
    }

    /**
     * Обновление записи
     *
     * @param element элемент, который необходимо обновить
     * @return кол-во обновленных записей
     */
    override fun update(element: CQCElementEntity): Int {
        return CQCElementTable.update({ CQCElementTable.id eq element.id }) {
            it[parentId] = element.parentId
            it[type] = element.type.id
            it[value] = element.value
        }
    }
}