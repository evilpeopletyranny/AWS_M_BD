package database.model.dao.table

import org.jetbrains.exposed.sql.Table

/**
 * Таблица уровней иерархии ККХ
 */
object CQCElementHierarchyTable : Table("cqc_elem_hierarchy") {
    val childId = uuid("child_type_id")
    val parentId = uuid("parent_type_id")

    override val primaryKey = PrimaryKey(childId, parentId, name = "child_parent_pk")
}