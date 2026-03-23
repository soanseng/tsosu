package app.tsosu.domain.repository

import app.tsosu.domain.model.Task

interface IcsExporter {
    fun exportTasks(tasks: List<Task>): String
}
