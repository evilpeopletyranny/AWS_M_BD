package database.model.dao.entity

import java.util.UUID

/**
 * Отображение таблицы связи курса и входных элементов ККХ.
 * Курс может быть связан с любым не листовым элементом ККХ.
 */
data class CourseInputLeafEntity(
    val courseId: UUID,
    val leafId: UUID
)
