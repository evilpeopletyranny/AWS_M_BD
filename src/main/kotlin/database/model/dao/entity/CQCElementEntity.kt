package database.model.dao.entity

import java.util.UUID

/**
 * Отображение элементов ККХ
 */
data class CQCElementEntity(
    val id: UUID,
    val parentId: UUID?,
    val type: CQCElementDictionaryEntity,
    val value: String
)