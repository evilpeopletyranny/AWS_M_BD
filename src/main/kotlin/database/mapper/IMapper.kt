package database.mapper

import database.entity.CQCElementDictionaryEntity
import database.entity.CQCElementDictionaryTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toCQCElementDictionaryEntity(): CQCElementDictionaryEntity = CQCElementDictionaryEntity(
    id = this[CQCElementDictionaryTable.id].value,
    name = this[CQCElementDictionaryTable.name]
)

sealed interface IMapper<EntityType> {
    fun row2entity(row: ResultRow): EntityType
}

interface ICQCElementDictionaryMapper : IMapper<CQCElementDictionaryEntity>