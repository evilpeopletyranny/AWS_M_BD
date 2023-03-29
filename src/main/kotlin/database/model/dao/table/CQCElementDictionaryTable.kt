package database.model.dao.table

import org.jetbrains.exposed.dao.id.UUIDTable

/**
 * Словарь элементов ККХ
 */
object CQCElementDictionaryTable : UUIDTable("cqc_elem_dict") {
    val name = varchar("name", 150)
    val isDeleted = bool("is_deleted")
}