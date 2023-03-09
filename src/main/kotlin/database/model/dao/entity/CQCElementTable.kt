package database.model.dao.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object CQCElementTable : UUIDTable("cqc_elem") {
    val parentId = reference("parent_id", CQCElementTable).nullable()
    val type = reference("type_id", CQCElementDictionaryTable).nullable()
    val value = varchar("value", 250)
}

data class CQCElementEntity(
    val id: UUID,
    val parentId: UUID?,
    val type: CQCElementDictionaryEntity,
    val value: String
)