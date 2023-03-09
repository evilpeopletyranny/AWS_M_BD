package database.model.domain

import java.util.UUID

data class CQCElement(
    val id: UUID,
    val parentId: UUID?,
    val type: CQCElementDictionary,
    val value: String
)
