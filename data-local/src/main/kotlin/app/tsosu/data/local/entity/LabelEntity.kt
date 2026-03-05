package app.tsosu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,
    val title: String,
    val color: String = "#4287f5",
)
