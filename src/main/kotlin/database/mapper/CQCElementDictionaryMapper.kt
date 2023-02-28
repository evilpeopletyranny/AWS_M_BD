package database.mapper

import database.entity.CQCElementDictionaryEntity
import database.entity.CQCElementDictionaryTable
import org.jetbrains.exposed.sql.ResultRow

object CQCElementDictionaryMapper : ICQCElementDictionaryMapper {
    override fun row2entity(row: ResultRow): CQCElementDictionaryEntity {
         return CQCElementDictionaryEntity(
            id = row[CQCElementDictionaryTable.id].value,
            name = row[CQCElementDictionaryTable.name]
        )
    }
}