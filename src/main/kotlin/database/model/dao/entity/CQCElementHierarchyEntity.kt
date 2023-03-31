package database.model.dao.entity

/**
 * Отображение уровня иерархии ККХ
 */
data class CQCElementHierarchyEntity(
    val parent: CQCElementDictionaryEntity,
    val child: CQCElementDictionaryEntity
)
