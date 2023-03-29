package database.model.dao.entity

import java.util.UUID

/**
 * Отображение Курса.
 * Имеет множества входных и выходных элементов ККХ.
 */
data class CourseEntity(
    val id: UUID,
    val name: String,
    val inputLeafs: Map<CQCElementDictionaryEntity, Set<CQCElementEntity>>,
    val outputLeafs: Map<CQCElementDictionaryEntity, Set<CQCElementEntity>>
)
