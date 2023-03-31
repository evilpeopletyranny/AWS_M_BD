package database.model.dao.repository

import database.model.dao.entity.*
import database.model.dao.table.CourseTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.ResultSet
import java.sql.SQLDataException
import java.util.*
import kotlin.collections.LinkedHashMap

/**
 * Реализация DAO для взаимодействия с таблицей курсов: course
 */
object CourseDAO : ICourseDAO {
    /**
     * Нахождение нужной колонки в таблице
     */
    private fun CourseTable.column(columnName: String): Column<*> {
        return columns.find { it.name == columnName } ?: throw SQLDataException("Unknown column name: $columnName")
    }

    /**
     * Преобразование переданной строки в native SQL и его исполнение.
     */
    private fun <T : Any> String.execAndMap(transform: (ResultSet) -> T): List<T> {
        val result = arrayListOf<T>()
        TransactionManager.current().exec(this) { rs ->
            while (rs.next()) {
                result += transform(rs)
            }
        }
        return result
    }

    private fun getInputElements(courseId: UUID): LinkedHashMap<CQCElementDictionaryEntity, Set<CQCElementEntity>> {
        val res = LinkedHashMap<CQCElementDictionaryEntity, Set<CQCElementEntity>>()

        val resultRow =  "select * from get_input_elements('${courseId}')".execAndMap { rs ->
            val parentId: String? = rs.getString("elem_parent_id")

            CQCElementEntity(
                id = UUID.fromString(rs.getString("elem_id")),
                parentId = if (parentId != null) UUID.fromString(parentId) else null,
                type = CQCElementDictionaryEntity(
                    id = UUID.fromString(rs.getString("type_id")),
                    name = rs.getString("type_name"),
                    isDeleted = rs.getBoolean("type_id_deleted")
                ),
                value = rs.getString("elem_value")
            )
        }.groupBy { it.type }.mapValues { it.value.toSet() }

        resultRow.forEach { (k, v) -> res[k] = v }
        return res
    }

    private fun getOutputElements(courseId: UUID): LinkedHashMap<CQCElementDictionaryEntity, Set<CQCElementEntity>> {
        val res = LinkedHashMap<CQCElementDictionaryEntity, Set<CQCElementEntity>>()

        val resultRow = "select * from get_output_elements('${courseId}')".execAndMap { rs ->
            val parentId: String? = rs.getString("elem_parent_id")

            CQCElementEntity(
                id = UUID.fromString(rs.getString("elem_id")),
                parentId = if (parentId != null) UUID.fromString(parentId) else null,
                type = CQCElementDictionaryEntity(
                    id = UUID.fromString(rs.getString("type_id")),
                    name = rs.getString("type_name"),
                    isDeleted = rs.getBoolean("type_id_deleted")
                ),
                value = rs.getString("elem_value")
            )
        }.groupBy { it.type }.mapValues { it.value.toSet() }

        resultRow.forEach { (k, v) -> res[k] = v }
        return res
    }

    private fun insertInputLeaf(element: CourseEntity) {
        element.inputLeafs.values.map { leafSet ->
            CourseInputLeafDAO.multiInsert(
                leafSet.map {
                    CourseInputLeafEntity(
                        courseId = element.id,
                        leafId = it.id
                    )
                }
            )
        }
    }

    private fun insertOutputLeaf(element: CourseEntity) {
        element.outputLeafs.values.map { leafSet ->
            CourseOutputLeafDAO.multiInsert(
                leafSet.map {
                    CourseOutputLeafEntity(
                        courseId = element.id,
                        leafId = it.id
                    )
                }
            )
        }
    }

    /**
     * Перевод результатов SQL запроса в отображение CQCElementHierarchyEntity
     */
    private fun ResultRow.toCourseEntity(): CourseEntity {
        val courseId = this[CourseTable.id].value

        return CourseEntity(
            id = courseId,
            name = this[CourseTable.name],
            inputLeafs = getInputElements(courseId),
            outputLeafs = getOutputElements(courseId)
        )
    }

    /**
     * Выбор всех записей таблицы.
     *
     * @param limit общее число записей выборки
     * @param offset отступ от первой записи
     * @param orderBy значение по которому необходимо провести сортировку
     * @param order порядок сортировки (ASC/DESC)
     * @return множество полученных элементов
     */
    override fun selectAll(
        limit: Int,
        offset: Long,
        orderBy: String,
        order: String
    ): Set<CourseEntity> {
        return CourseTable
            .selectAll()
            .limit(limit, offset)
            .orderBy(CourseTable.column(orderBy) to SortOrder.valueOf(order))
            .map { it.toCourseEntity() }
            .toSet()
    }

    /**
     * Выбор записи по ID
     *
     * @param id первичный ключ, по которому осуществляется поиск
     * @return nullable элемент (null если такой записи в таблице нет)
     */
    override fun selectById(id: UUID): CourseEntity? {
        return CourseTable.select {
            CourseTable.id eq id
        }.firstOrNull()?.toCourseEntity()
    }

    /**
     * Вставка записи
     *
     * @param element элемент для вставки
     * @return nullable id вставленного элемента
     */
    override fun insert(element: CourseEntity): UUID? {
        val insertedCount = CourseTable.insert {
            it[id] = element.id
            it[name] = element.name
        }.insertedCount

        insertInputLeaf(element)
        insertOutputLeaf(element)

        return if (insertedCount > 0) element.id else null
    }

    /**
     * Вставка нескольких записей
     *
     * @param elements элементы для вставки
     */
    override fun multiInsert(elements: Collection<CourseEntity>): List<ResultRow> {
        val res = CourseTable.batchInsert(elements) {
            this[CourseTable.id] = it.id
            this[CourseTable.name] = it.name
        }

        elements.forEach { course ->
            insertInputLeaf(course)
            insertOutputLeaf(course)
        }

        return res
    }

    /**
     * Удаление записи по ID
     *
     * @param id первичный ключ по которому выполнится удаление
     * @return кол-во удаленных записей
     */
    override fun deleteById(id: UUID): Int {
        return CourseTable.deleteWhere { CourseTable.id eq id }
    }

    /**
     * Обновление записи
     *
     * @param element элемент, который необходимо обновить
     * @return кол-во обновленных записей
     */
    override fun update(element: CourseEntity): Int {
        CourseInputLeafDAO.deleteByCourseId(element.id)
        CourseOutputLeafDAO.deleteByCourseId(element.id)

        val res = CourseTable.update({ CourseTable.id eq element.id })
        {
            it[name] = element.name
        }

        insertInputLeaf(element)
        insertOutputLeaf(element)

        return res
    }
}