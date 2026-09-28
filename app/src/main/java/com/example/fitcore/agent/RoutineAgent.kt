package com.example.fitcore.agent

import com.example.fitcore.db.UserEntity

enum class RoutinePlanMode {
    FullBody,
    Weekly,
    Suggested
}

data class PlannedExercise(
    val exerciseId: Int,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int,
    val orderIndex: Int,
    val dayLabel: String
)

data class RoutinePlan(
    val name: String,
    val description: String,
    val isGenerated: Boolean = true,
    val exercises: List<PlannedExercise>
)

interface RoutineAgent {
    suspend fun plan(user: UserEntity, mode: RoutinePlanMode): RoutinePlan
}
