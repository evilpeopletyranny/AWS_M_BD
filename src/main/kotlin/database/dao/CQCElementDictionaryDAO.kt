package database.dao

import database.entity.CQCElementDictionaryEntity
import database.entity.CQCElementDictionaryTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.sql.SQLDataException
import java.util.*

object CQCElementDictionaryDAO : ICQCElementDictionaryDAO {
    private fun CQCElementDictionaryTable.column(columnName: String): Column<*> {
        return columns.find { it.name == columnName } ?: throw SQLDataException("Unknown column name: $columnName")
    }

    private fun ResultRow.toCQCElementDictionaryEntity(): CQCElementDictionaryEntity = CQCElementDictionaryEntity(
        id = this[CQCElementDictionaryTable.id].value,
        name = this[CQCElementDictionaryTable.name]
    )

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

    override fun selectById(id: UUID): CQCElementDictionaryEntity? {
        return CQCElementDictionaryTable.select {
            CQCElementDictionaryTable.id eq id
        }.firstOrNull()?.toCQCElementDictionaryEntity()
    }

    override fun insert(element: CQCElementDictionaryEntity): UUID? {
        val insertedCount = CQCElementDictionaryTable.insert {
            it[id] = element.id
            it[name] = element.name
        }.insertedCount

        return if (insertedCount > 0) element.id else null
    }

    override fun multiInsert(elements: Collection<CQCElementDictionaryEntity>): List<ResultRow> {
        return CQCElementDictionaryTable.batchInsert(elements) {
            this[CQCElementDictionaryTable.id] = it.id
            this[CQCElementDictionaryTable.name] = it.name
        }
    }

    override fun deleteById(id: UUID): Int {
        return CQCElementDictionaryTable.deleteWhere { CQCElementDictionaryTable.id eq id }
    }

    override fun update(element: CQCElementDictionaryEntity): Int {
        return CQCElementDictionaryTable.update({ CQCElementDictionaryTable.id eq element.id })
        { it[name] = element.name }
    }
}
