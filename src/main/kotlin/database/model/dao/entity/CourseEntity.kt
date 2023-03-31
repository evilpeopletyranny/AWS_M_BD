package database.model.dao.entity


import java.util.UUID

/**
 * Отображение Курса.
 * Имеет множества входных и выходных элементов ККХ.
 */
data class CourseEntity(
    val id: UUID,
    val name: String,
    val inputLeafs: LinkedHashMap<CQCElementDictionaryEntity, Set<CQCElementEntity>>,
    val outputLeafs: LinkedHashMap<CQCElementDictionaryEntity, Set<CQCElementEntity>>
) {
    fun checkHierarchy(hierarchy: LinkedHashSet<CQCElementDictionaryEntity>) {
        hierarchy.map { type ->
            inputLeafs[type].let { elements ->
                elements?.let {
                    inputLeafs.put(type, elements)
                }
            }

            outputLeafs[type].let { elements ->
                elements?.let {
                    outputLeafs.put(type, elements)
                }
            }
        }
    }
    override fun toString(): String {
        return """${name}. 
            |Входные  элементы ККХ: ${inputLeafs.map { entry -> Pair(entry.key.name, entry.value.map { it.value }) }}
            |Выходные элементы ККХ: ${
            outputLeafs.map { entry ->
                Pair(
                    entry.key.name,
                    entry.value.map { it.value })
            }
        }""".trimMargin()
    }
}
