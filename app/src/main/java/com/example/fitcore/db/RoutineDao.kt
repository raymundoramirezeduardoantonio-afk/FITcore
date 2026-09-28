package com.example.fitcore.db

import kotlinx.coroutines.flow.Flow
import androidx.room.*

data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val exercises: List<RoutineExerciseEntity>
)
@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT r.* FROM routines r INNER JOIN user_profile u ON r.userId = u.id WHERE u.isLoggedIn = 1 ORDER BY r.createdAt DESC")
    fun getAllRoutines(): Flow<List<RoutineWithExercises>>

    @Query("SELECT * FROM user_profile WHERE isLoggedIn = 1 LIMIT 1")
    fun getLoggedInUserFlow(): Flow<UserEntity?>


    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineById(routineId: Long): RoutineWithExercises?

    @Insert
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Insert
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercises(routineId: Long)

    // Perfil de Usuario y Login
    @Query("SELECT * FROM user_profile WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserEntity?

    @Query("SELECT * FROM user_profile WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(user: UserEntity): Long

    @Query("UPDATE user_profile SET isLoggedIn = 0")
    suspend fun logoutAll()

    @Query("SELECT * FROM user_profile WHERE id = 1") 
    suspend fun getUserProfile(): UserEntity?

    @Query("UPDATE user_profile SET xp = xp + :xpPoints, workoutsCompleted = workoutsCompleted + 1 WHERE id = :userId")
    suspend fun addXpToUser(userId: Int, xpPoints: Int)

    @Query("DELETE FROM routines WHERE isGenerated = 1 AND userId = (SELECT id FROM user_profile WHERE isLoggedIn = 1 LIMIT 1)")
    suspend fun deleteGeneratedRoutines()

    @Query("UPDATE user_profile SET streakDays = :days, lastWorkoutTimestamp = :timestamp WHERE id = :userId")
    suspend fun updateStreak(userId: Int, days: Int, timestamp: Long)
}
