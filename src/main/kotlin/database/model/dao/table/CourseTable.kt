package database.model.dao.table

import org.jetbrains.exposed.dao.id.UUIDTable

/**
 * Таблица Курсов
 */
object CourseTable : UUIDTable("course") {
    val name = varchar("name", 250)
}