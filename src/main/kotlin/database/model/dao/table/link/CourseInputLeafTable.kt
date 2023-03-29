package database.model.dao.table.link

import org.jetbrains.exposed.sql.Table

/**
 * Таблица связи курсов и входных элементов ККХ.
 * Курс может связываться с любыми не листовыми элементами структуры ККХ
 */
object CourseInputLeafTable : Table("course_input_leaf_link") {
    val courseId = uuid("course_id")
    val leafId = uuid("leaf_id")

    override val primaryKey = PrimaryKey(courseId, leafId, name = "course_input_leaf_pk")
}