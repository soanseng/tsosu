package app.tsosu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,
    val title: String,
    val color: String = "#808080",
    val parentProjectId: String? = null,
    val position: Double = 0.0,
    val isFavorite: Boolean = false,
    val isRoutine: Boolean = false,
)
