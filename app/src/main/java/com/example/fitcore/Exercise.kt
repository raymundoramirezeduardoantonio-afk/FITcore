package com.example.fitcore

import java.io.Serializable

data class Exercise(
    val id: Int,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val difficulty: String,
    val instructions: String,
    val imageUrl: String = ""
) : Serializable
