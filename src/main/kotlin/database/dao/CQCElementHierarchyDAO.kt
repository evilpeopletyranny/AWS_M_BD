package database.dao

import database.entity.CQCElementHierarchyEntity
import database.entity.CQCElementHierarchyTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.sql.SQLDataException
import java.util.*

object CQCElementHierarchyDAO : ICQCElementHierarchyDAO {
    private fun CQCElementHierarchyTable.column(columnName: String): Column<*> {
        return columns.find { it.name == columnName } ?: throw SQLDataException("Unknown column name: $columnName")
    }

    private fun ResultRow.toCQCElementHierarchyEntity(): CQCElementHierarchyEntity = CQCElementHierarchyEntity(
        childId = this[CQCElementHierarchyTable.childId],
        parentId = this[CQCElementHierarchyTable.parentId]
    )

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

    override fun selectById(id: UUID): CQCElementHierarchyEntity? {
        return CQCElementHierarchyTable.select {
            CQCElementHierarchyTable.childId eq id
        }.firstOrNull()?.toCQCElementHierarchyEntity()
    }

    override fun insert(element: CQCElementHierarchyEntity): UUID? {
        val insertedCount = CQCElementHierarchyTable.insert {
            it[childId] = element.childId
            it[parentId] = element.parentId
        }.insertedCount

        return if (insertedCount > 0) element.childId else null
    }

    override fun multiInsert(elements: Collection<CQCElementHierarchyEntity>): List<ResultRow> {
        return CQCElementHierarchyTable.batchInsert(elements) {
            this[CQCElementHierarchyTable.childId] = it.childId
            this[CQCElementHierarchyTable.parentId] = it.parentId
        }
    }

    override fun deleteById(id: UUID): Int {
        return CQCElementHierarchyTable.deleteWhere { childId eq id }
    }

    fun deleteHierarchyLevel(entity: CQCElementHierarchyEntity): Int {
        return CQCElementHierarchyTable.deleteWhere {
            Op.build { childId eq entity.childId and (parentId eq entity.parentId) }
        }
    }

    override fun update(element: CQCElementHierarchyEntity): Int {
        return CQCElementHierarchyTable.update({ CQCElementHierarchyTable.childId eq element.childId })
        { it[parentId] = element.parentId }
    }
}