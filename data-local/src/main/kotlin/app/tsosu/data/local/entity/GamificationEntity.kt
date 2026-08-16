package app.tsosu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row local gamification state (id is always 1). */
@Entity(tableName = "gamification")
data class GamificationEntity(
    @PrimaryKey val id: Int = 1,
    val energy: Int = 0,
    val freezes: Int = 0,
)
