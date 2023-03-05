package database.entity

import org.jetbrains.exposed.sql.Table
import java.util.*

/**
 * Таблица уровней иерархии ККХ
 */
object CQCElementHierarchyTable : Table("cqc_elem_hierarchy") {
    val childId = uuid("child_type_id")
    val parentId = uuid("parent_type_id")

    override val primaryKey = PrimaryKey(childId, name = "child_parent_pk")
}

/**
 * Отображение уровня иерархии ККХ
 */
data class CQCElementHierarchyEntity(
    val parentId: UUID,
    val childId: UUID
)
