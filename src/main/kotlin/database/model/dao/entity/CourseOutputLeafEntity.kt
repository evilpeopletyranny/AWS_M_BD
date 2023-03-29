package database.model.dao.entity

import java.util.*

/**
 * Отображение таблицы связи курса и выходных элементов ККХ.
 * Курс может быть связан с любым не листовым элементом ККХ.
 */
data class CourseOutputLeafEntity(
    val courseId: UUID,
    val leafId: UUID
)