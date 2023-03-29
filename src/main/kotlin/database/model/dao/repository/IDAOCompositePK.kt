package database.model.dao.repository

import database.model.dao.entity.CQCElementHierarchyEntity
import database.model.dao.entity.CourseInputLeafEntity
import database.model.dao.entity.CourseOutputLeafEntity
import org.jetbrains.exposed.sql.ResultRow
import java.util.*

sealed interface IDAOCompositePK<EntityType> {
    /**
     * Выбор всех записей таблицы.
     *
     * @param limit общее число записей выборки
     */
    fun selectAll(
        limit: Int = 100,
    ): Set<EntityType>

    /**
     * Выбор записи по Primary Key
     *
     * @param firstPart первая часть составного ключа
     * @param secondPart вторая часть составного ключа
     */
    fun selectByPK(
        firstPart: UUID,
        secondPart: UUID
    ): EntityType?

    /**
     * Вставка записи
     *
     * @param element элемент для вставки
     */
    fun insert(
        element: EntityType,
    ): Int

    //TODO("Посмотреть про batch insert")
    //TODO("Результат?!")
    /**
     * Вставка нескольких записей
     *
     * @param elements элементы для вставки
     */
    fun multiInsert(
        elements: Collection<EntityType>,
    ): List<ResultRow>

    /**
     * Удаление записи по Primary Key
     *
     * @param firstPart первая часть составного ключа
     * @param secondPart вторая часть составного ключа
     */
    fun deleteByPK(
        firstPart: UUID,
        secondPart: UUID
    ): Int
}

/**
 * Интерфейс DAO для взаимодействия с уровнями ККХ: cqc_elem_hierarchy
 */
interface ICQCElementHierarchyDAO : IDAOCompositePK<CQCElementHierarchyEntity>

/**
 * Интерфейс DAO для взаимодействия с таблицей связи курсов и входных элементов ККХ: course_input_leaf_link
 */
interface ICourseInputLeafDAO : IDAOCompositePK<CourseInputLeafEntity>

/**
 * Интерфейс DAO для взаимодействия с таблицей связи курсов и выходных элементов ККХ: course_output_leaf_link
 */
interface ICourseOutputLeafDAO : IDAOCompositePK<CourseOutputLeafEntity>