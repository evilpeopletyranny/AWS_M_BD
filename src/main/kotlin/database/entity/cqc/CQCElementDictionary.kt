package database.entity.cqc

import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

/**
 * Словарь элементов ККХ
 */
object CQCElementDictionaryTable : UUIDTable("cqc_elem_dict") {
    val name = varchar("name", 150)
}

/**
 * Отображение словаря элементов ККХ
 */
data class CQCElementDictionaryEntity(
    val id: UUID,
    val name: String
)