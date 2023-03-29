package database.model.dao.table.link

import org.jetbrains.exposed.sql.Table

/**
 * Таблица связи курсов и выходных элементов ККХ.
 * Курс может связываться с любыми не листовыми элементами структуры ККХ
 */
object CourseOutputLeafTable : Table("course_output_leaf_link") {
    val courseId = uuid("course_id")
    val leafId = uuid("leaf_id")

    override val primaryKey = PrimaryKey(courseId, leafId, name = "course_output_leaf_pk")
}