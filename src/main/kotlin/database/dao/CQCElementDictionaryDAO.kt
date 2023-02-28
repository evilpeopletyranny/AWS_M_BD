package database.dao

import database.entity.CQCElementDictionaryEntity
import database.entity.CQCElementDictionaryTable
import database.mapper.toCQCElementDictionaryEntity
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.lang.Exception
import java.util.*

fun CQCElementDictionaryTable.column(columName: String): Column<*> {
    return columns.find { it.name == columName } ?: throw Exception("A")
}

object CQCElementDictionaryDAO : ICQCElementDictionaryDAO {
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

    override fun insert(element: CQCElementDictionaryEntity): UUID {
        return CQCElementDictionaryTable.insertAndGetId {
            it[id] = element.id
            it[name] = element.name
        }.value
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
