package database.model.dao.entity

import java.util.UUID

/**
 * Отображение словаря элементов ККХ
 */
data class CQCElementDictionaryEntity(
    val id: UUID,
    val name: String,
    val isDeleted: Boolean
)