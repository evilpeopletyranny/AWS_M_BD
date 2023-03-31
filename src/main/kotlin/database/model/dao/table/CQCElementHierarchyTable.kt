package database.model.dao.table

import org.jetbrains.exposed.sql.Table

/**
 * Таблица уровней иерархии ККХ
 */
object CQCElementHierarchyTable : Table("cqc_elem_hierarchy") {
    val child = reference("child_type_id", CQCElementDictionaryTable)
    val parent = reference("parent_type_id", CQCElementDictionaryTable)

    override val primaryKey = PrimaryKey(child, parent, name = "child_parent_pk")
}