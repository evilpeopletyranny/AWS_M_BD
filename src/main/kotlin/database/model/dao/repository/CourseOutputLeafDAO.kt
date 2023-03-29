package database.model.dao.repository

import database.model.dao.entity.CourseOutputLeafEntity
import database.model.dao.table.link.CourseInputLeafTable
import database.model.dao.table.link.CourseOutputLeafTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Реализация DAO для взаимодействия с таблицей связи курсов и выходных элементов ККХ: course_output_leaf_link
 */
object CourseOutputLeafDAO : ICourseOutputLeafDAO {
    /**
     * Перевод результатов SQL запроса в отображение CourseOutputLeafEntity
     */
    private fun ResultRow.toCourseOutputLeafEntity(): CourseOutputLeafEntity = CourseOutputLeafEntity(
        courseId = this[CourseOutputLeafTable.courseId],
        leafId = this[CourseOutputLeafTable.leafId]
    )

    /**
     * Выбор записи по Primary Key
     *
     * @param firstPart courseId
     * @param secondPart leafId
     * @return nullable элемент (null если такой записи в таблице нет)
     */
    override fun selectByPK(firstPart: UUID, secondPart: UUID): CourseOutputLeafEntity? {
        return CourseOutputLeafTable.select {
            Op.build { CourseOutputLeafTable.courseId eq firstPart and (CourseOutputLeafTable.leafId eq secondPart) }
        }.firstOrNull()?.toCourseOutputLeafEntity()
    }

    /**
     * Удаление записи по Primary Key
     *
     * @param firstPart courseId
     * @param secondPart leafId
     * @return кол-во удаленных записей
     */
    override fun deleteByPK(firstPart: UUID, secondPart: UUID): Int {
        return CourseOutputLeafTable.deleteWhere {
            Op.build { courseId eq firstPart and (leafId eq secondPart) }
        }
    }

    /**
     * Удаление записи по id Курса
     *
     * @param id идентификатор курса, связи с которым необходимо удалить
     * @return кол-во удаленных записей
     */
    fun deleteByCourseId(id: UUID): Int {
        return CourseOutputLeafTable.deleteWhere {
            courseId eq id
        }
    }

    /**
     * Выбор всех записей таблицы.
     *
     * @param limit общее число записей выборки
     * @return множество полученных элементов
     */
    override fun selectAll(limit: Int): Set<CourseOutputLeafEntity> {
        return CourseOutputLeafTable.selectAll()
            .limit(limit)
            .map { it.toCourseOutputLeafEntity() }
            .toSet()
    }

    override fun insert(element: CourseOutputLeafEntity): Int {
        return CourseOutputLeafTable.insert {
            it[courseId] = element.courseId
            it[leafId] = element.leafId
        }.insertedCount
    }

    override fun multiInsert(elements: Collection<CourseOutputLeafEntity>): List<ResultRow> {
        return CourseOutputLeafTable.batchInsert(elements) {
            this[CourseOutputLeafTable.courseId] = it.courseId
            this[CourseOutputLeafTable.leafId] = it.leafId
        }
    }
}