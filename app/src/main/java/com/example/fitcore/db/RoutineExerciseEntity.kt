package com.example.fitcore.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_exercises",
    indices = [Index(value = ["routineId", "exerciseId", "orderIndex"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Int,
    val sets: Int = 3,
    val reps: Int = 10,
    val restSeconds: Int = 60,
    val orderIndex: Int = 0,
    val dayLabel: String = ""
)
