package database.mapper

import database.entity.CQCElementDictionaryEntity
import org.jetbrains.exposed.sql.ResultRow


sealed interface IMapper<EntityType> {
    fun row2entity(row: ResultRow): EntityType
}

interface ICQCElementDictionaryMapper : IMapper<CQCElementDictionaryEntity>