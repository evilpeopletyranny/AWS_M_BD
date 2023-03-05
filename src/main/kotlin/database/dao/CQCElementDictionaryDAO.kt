package database.dao

import database.entity.CQCElementDictionaryEntity
import database.entity.CQCElementDictionaryTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.sql.SQLDataException
import java.util.*

/**
 * Реализация DAO для взаимодействия со словарём элементов ККХ: cqc_elem_dict
 */
object CQCElementDictionaryDAO : ICQCElementDictionaryDAO {
    /**
     * Нахождение нужной колонки в таблице
     */
    private fun CQCElementDictionaryTable.column(columnName: String): Column<*> {
        return columns.find { it.name == columnName } ?: throw SQLDataException("Unknown column name: $columnName")
    }

    /**
     * Перевод результатов SQL запроса в отображение CQCElementDictionaryEntity
     */
    private fun ResultRow.toCQCElementDictionaryEntity(): CQCElementDictionaryEntity = CQCElementDictionaryEntity(
        id = this[CQCElementDictionaryTable.id].value,
        name = this[CQCElementDictionaryTable.name]
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
    ): Set<CQCElementDictionaryEntity> {
        return CQCElementDictionaryTable
            .selectAll()
            .limit(limit, offset)
            .orderBy(CQCElementDictionaryTable.column(orderBy) to SortOrder.valueOf(order))
            .map { it.toCQCElementDictionaryEntity() }
            .toSet()
    }

    /**
     * Выбор записи по ID
     *
     * @param id первичный ключ, по которому осуществляется поиск
     * @return nullable элемент (null если такой записи в таблице нет)
     */
    override fun selectById(id: UUID): CQCElementDictionaryEntity? {
        return CQCElementDictionaryTable.select {
            CQCElementDictionaryTable.id eq id
        }.firstOrNull()?.toCQCElementDictionaryEntity()
    }

    /**
     * Вставка записи
     *
     * @param element элемент для вставки
     * @return nullable id вставленного элемента
     */
    override fun insert(element: CQCElementDictionaryEntity): UUID? {
        val insertedCount = CQCElementDictionaryTable.insert {
            it[id] = element.id
            it[name] = element.name
        }.insertedCount

        return if (insertedCount > 0) element.id else null
    }

    /**
     * Вставка нескольких записей
     *
     * @param elements элементы для вставки
     */
    override fun multiInsert(elements: Collection<CQCElementDictionaryEntity>): List<ResultRow> {
        return CQCElementDictionaryTable.batchInsert(elements) {
            this[CQCElementDictionaryTable.id] = it.id
            this[CQCElementDictionaryTable.name] = it.name
        }
    }

    /**
     * Удаление записи по ID
     *
     * @param id первичный ключ по которому выполнится удаление
     * @return кол-во удаленных записей
     */
    override fun deleteById(id: UUID): Int {
        return CQCElementDictionaryTable.deleteWhere { CQCElementDictionaryTable.id eq id }
    }

    /**
     * Обновление записи
     *
     * @param element элемент, который необходимо обновить
     * @return кол-во обновленных записей
     */
    override fun update(element: CQCElementDictionaryEntity): Int {
        return CQCElementDictionaryTable.update({ CQCElementDictionaryTable.id eq element.id })
        { it[name] = element.name }
    }
}
