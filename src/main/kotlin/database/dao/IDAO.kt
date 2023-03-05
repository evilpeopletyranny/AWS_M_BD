package database.dao

import database.entity.CQCElementDictionaryEntity
import database.entity.CQCElementHierarchyEntity
import org.jetbrains.exposed.sql.ResultRow
import java.util.*

/**
 * Обобщенный интерфейс объектов доступа к сущностям БД
 * @param EntityType тип объектов отображающий содержимое таблицы.
 */
sealed interface IDAO<EntityType> {
    /**
     * Выбор всех записей таблицы.
     *
     * @param limit общее число записей выборки
     * @param offset отступ от первой записи
     * @param orderBy значение по которому необходимо провести сортировку
     * @param order порядок сортировки (ASC/DESC)
     */
    fun selectAll(
        limit: Int = 100,
        offset: Long = 0,
        orderBy: String,
        order: String = "ASC"
    ): Set<EntityType>

    /**
     * Выбор записи по ID
     *
     * @param id первичный ключ, по которому осуществляется поиск
     */
    fun selectById(
        id: UUID
    ): EntityType?

    /**
     * Вставка записи
     *
     * @param element элемент для вставки
     */
    fun insert(
        element: EntityType,
    ): UUID?

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
     * Удаление записи по ID
     *
     * @param id первичный ключ по которому выполнится удаление
     */
    fun deleteById(
        id: UUID,
    ): Int

    /**
     * Обновление записи
     *
     * @param element элемент, который необходимо обновить
     */
    fun update(
        element: EntityType,
    ): Int
}

/**
 * Интерфейс DAO для взаимодействия со словарём элементов ККХ: cqc_elem_dict
 */
interface ICQCElementDictionaryDAO : IDAO<CQCElementDictionaryEntity>

/**
 * Интерфейс DAO для взаимодействия с уровнями ККХ: cqc_elem_hierarchy
 */
interface ICQCElementHierarchyDAO : IDAO<CQCElementHierarchyEntity>