package app.tsosu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.tsosu.data.local.entity.HabitCompletionEntity
import app.tsosu.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity)

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE id = :habitId")
    fun getById(habitId: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY position")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE routineId = :routineId AND isArchived = 0 ORDER BY position")
    fun getByRoutine(routineId: String): Flow<List<HabitEntity>>

    @Query("UPDATE habits SET isArchived = 1 WHERE id = :habitId")
    suspend fun archive(habitId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteCompletion(habitId: String, date: Long)

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun getCompletionsForDate(date: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getCompletionsForHabit(habitId: String, startDate: Long, endDate: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate")
    fun getCompletionCount(habitId: String, startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    fun getAllCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>
}
