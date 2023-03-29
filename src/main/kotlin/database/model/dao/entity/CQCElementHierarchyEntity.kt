package database.model.dao.entity

import java.util.UUID

/**
 * Отображение уровня иерархии ККХ
 */
data class CQCElementHierarchyEntity(
    val parentId: UUID,
    val childId: UUID
)
