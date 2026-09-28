package com.example.fitcore.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val height: Float = 0f,
    val weight: Float = 0f,
    val bodyType: String = "",
    val isLoggedIn: Boolean = false,
    // Campos para Gamificación
    val xp: Int = 0,
    val level: Int = 1,
    val streakDays: Int = 0,
    val lastWorkoutTimestamp: Long = 0,
    val workoutsCompleted: Int = 0
)
