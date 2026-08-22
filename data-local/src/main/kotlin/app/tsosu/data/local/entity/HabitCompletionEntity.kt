package app.tsosu.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.Index
import androidx.room.PrimaryKey

@Serializable
@Entity(
    tableName = "habit_completions",
    indices = [Index(value = ["habitId", "date"], unique = true)],
)
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val date: Long,
    val completedAt: Long,
)
