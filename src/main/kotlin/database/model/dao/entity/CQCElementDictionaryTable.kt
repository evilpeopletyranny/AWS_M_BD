package database.model.dao.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

/**
 * Словарь элементов ККХ
 */
object CQCElementDictionaryTable : UUIDTable("cqc_elem_dict") {
    val name = varchar("name", 150)
    val isDeleted = bool("is_deleted")
}

/**
 * Отображение словаря элементов ККХ
 */
data class CQCElementDictionaryEntity(
    val id: UUID,
    val name: String,
    val isDeleted: Boolean
)