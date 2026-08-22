package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task

/**
 * Pure digest composition (No-Shame tone). Returns structured content; the
 * app layer localizes the notification text from it.
 */
object DigestFormatter {

    data class DigestContent(
        /** Up to N pending task titles for the morning plan. */
        val topPendingTitles: List<String>,
        val completedTodayCount: Int,
        val habitsCompleted: Int,
        val habitsTotal: Int,
    )

    fun build(
        todayTasks: List<Task>,
        habitsCompleted: Int,
        habitsTotal: Int,
        maxTitles: Int = 3,
    ): DigestContent = DigestContent(
        topPendingTitles = todayTasks
            .filter { !it.status.isTerminal }
            .take(maxTitles)
            .map { it.title },
        completedTodayCount = todayTasks.count { it.status.isDone },
        habitsCompleted = habitsCompleted,
        habitsTotal = habitsTotal,
    )
}
