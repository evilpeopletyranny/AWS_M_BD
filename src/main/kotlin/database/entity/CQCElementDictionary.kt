package database.entity

import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object CQCElementDictionaryTable : UUIDTable("cqc_elem_dict") {
    val name = varchar("name", 150)
}

data class CQCElementDictionaryEntity(
    val id: UUID,
    val name: String
)