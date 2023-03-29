package database.model.dao.repository

import database.model.dao.entity.CourseInputLeafEntity
import database.model.dao.table.CourseTable
import database.model.dao.table.link.CourseInputLeafTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

/**
 * Реализация DAO для взаимодействия с таблицей связи курсов и входных элементов ККХ: course_input_leaf_link
 */
object CourseInputLeafDAO : ICourseInputLeafDAO {
    /**
     * Перевод результатов SQL запроса в отображение CourseInputLeafEntity
     */
    private fun ResultRow.toCourseInputLeafEntity(): CourseInputLeafEntity = CourseInputLeafEntity(
        courseId = this[CourseInputLeafTable.courseId],
        leafId = this[CourseInputLeafTable.leafId]
    )

    /**
     * Выбор записи по Primary Key
     *
     * @param firstPart courseId
     * @param secondPart leafId
     * @return nullable элемент (null если такой записи в таблице нет)
     */
    override fun selectByPK(firstPart: UUID, secondPart: UUID): CourseInputLeafEntity? {
        return CourseInputLeafTable.select {
            Op.build { CourseInputLeafTable.courseId eq firstPart and (CourseInputLeafTable.leafId eq secondPart) }
        }.firstOrNull()?.toCourseInputLeafEntity()
    }

    /**
     * Выбор всех связей с указанным курсом
     *
     * @param id идентификатор курса
     * @return множество
     */
    fun selectByCourseId(id: UUID): Set<CourseInputLeafEntity> {
        return CourseInputLeafTable.selectAll()
            .adjustWhere { CourseTable.id eq id }
            .map { it.toCourseInputLeafEntity() }
            .toSet()
    }

    /**
     * Удаление записи по Primary Key
     *
     * @param firstPart courseId
     * @param secondPart leafId
     * @return кол-во удаленных записей
     */
    override fun deleteByPK(firstPart: UUID, secondPart: UUID): Int {
        return CourseInputLeafTable.deleteWhere {
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
        return CourseInputLeafTable.deleteWhere {
            courseId eq id
        }
    }

    /**
     * Выбор всех записей таблицы.
     *
     * @param limit общее число записей выборки
     * @return множество полученных элементов
     */
    override fun selectAll(limit: Int): Set<CourseInputLeafEntity> {
        return CourseInputLeafTable.selectAll()
            .limit(limit)
            .map { it.toCourseInputLeafEntity() }
            .toSet()
    }

    /**
     * Вставка записи
     *
     * @param element элемент для вставки
     * @return nullable id вставленного элемента
     */
    override fun insert(element: CourseInputLeafEntity): Int {
        return CourseInputLeafTable.insert {
            it[courseId] = element.courseId
            it[leafId] = element.leafId
        }.insertedCount
    }

    /**
     * Вставка нескольких записей
     *
     * @param elements элементы для вставки
     */
    override fun multiInsert(elements: Collection<CourseInputLeafEntity>): List<ResultRow> {
        return CourseInputLeafTable.batchInsert(elements) {
            this[CourseInputLeafTable.courseId] = it.courseId
            this[CourseInputLeafTable.leafId] = it.leafId
        }
    }
}