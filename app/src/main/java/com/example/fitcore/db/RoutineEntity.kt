package com.example.fitcore.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int = 0, // Nuevo: Vincula la rutina a un usuario específico
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isGenerated: Boolean = false
)
