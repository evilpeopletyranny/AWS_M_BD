package database.entity

import org.jetbrains.exposed.sql.Table
import java.util.*

object CQCElementHierarchyTable : Table("cqc_elem_hierarchy") {
    val childId = uuid("child_type_id")
    val parentId = uuid("parent_type_id")

    override val primaryKey = PrimaryKey(childId, name = "child_parent_pk")
}

data class CQCElementHierarchyEntity(
    val parentId: UUID,
    val childId: UUID
)
