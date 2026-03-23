package app.tsosu.domain.model

import kotlinx.datetime.LocalDate

data class FilterSpec(
    val statuses: Set<TaskStatus>? = null,
    val minPriority: Priority? = null,
    val energyLevels: Set<EnergyLevel>? = null,
    val projectIds: Set<String>? = null,
    val dueDateFrom: LocalDate? = null,
    val dueDateTo: LocalDate? = null,
    val titleContains: String? = null,
    val hasDescription: Boolean? = null,
) {
    fun apply(tasks: List<Task>): List<Task> = tasks.filter { task ->
        (statuses == null || task.status in statuses) &&
            (minPriority == null || task.priority.value >= minPriority.value) &&
            (energyLevels == null || task.energyLevel in energyLevels) &&
            (projectIds == null || task.projectId in projectIds) &&
            (dueDateFrom == null || (task.dueDate != null && task.dueDate.date >= dueDateFrom)) &&
            (dueDateTo == null || (task.dueDate != null && task.dueDate.date <= dueDateTo)) &&
            (titleContains == null || task.title.contains(titleContains, ignoreCase = true)) &&
            (hasDescription == null || (task.description.isNotBlank() == hasDescription))
    }
}

enum class SortField { PRIORITY, DUE_DATE, CREATED, TITLE, ENERGY, STATUS }

data class SortSpec(
    val field: SortField = SortField.DUE_DATE,
    val ascending: Boolean = true,
) {
    fun apply(tasks: List<Task>): List<Task> {
        val comparator: Comparator<Task> = when (field) {
            SortField.PRIORITY -> compareBy { it.priority.value }
            SortField.DUE_DATE -> compareBy(nullsLast()) { it.dueDate }
            SortField.CREATED -> compareBy { it.createdAt }
            SortField.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortField.ENERGY -> compareBy { it.energyLevel.ordinal }
            SortField.STATUS -> compareBy { it.status.ordinal }
        }
        return if (ascending) tasks.sortedWith(comparator) else tasks.sortedWith(comparator.reversed())
    }
}
