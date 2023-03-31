package database.model.dao.table

import org.jetbrains.exposed.dao.id.UUIDTable

/**
 * Таблица элементов ККХ
 */
object CQCElementTable : UUIDTable("cqc_elem") {
    val parentId = reference("parent_id", CQCElementTable).nullable()
    val type = reference("type_id", CQCElementDictionaryTable)
    val value = varchar("value", 250)
}