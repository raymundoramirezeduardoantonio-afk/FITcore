package com.example.fitcore.agent

import com.example.fitcore.ExerciseData

object RoutinePlanParser {
    fun allowedDayLabels(mode: RoutinePlanMode): Set<String> = when (mode) {
        RoutinePlanMode.FullBody, RoutinePlanMode.Suggested -> setOf("Todo")
        RoutinePlanMode.Weekly -> setOf("Lunes", "Miércoles", "Viernes")
    }

    fun extractAssistantContent(responseBody: String): String? {
        val root = JsonText.parse(responseBody) as? JsonValue.Obj ?: return null
        val choices = root.map["choices"] as? JsonValue.Arr ?: return null
        val first = choices.items.firstOrNull() as? JsonValue.Obj ?: return null
        val message = first.map["message"] as? JsonValue.Obj ?: return null
        val content = (message.map["content"] as? JsonValue.Str)?.text?.trim().orEmpty()
        return content.ifBlank { null }
    }

    fun parse(json: String, mode: RoutinePlanMode, bodyType: String = ""): RoutinePlan? {
        val root = JsonText.parse(unwrap(json)) as? JsonValue.Obj ?: return null
        val exercisesJson = root.map["exercises"] as? JsonValue.Arr ?: return null
        if (exercisesJson.items.isEmpty()) return null

        val allowed = allowedDayLabels(mode)
        val seen = mutableSetOf<Int>()
        val exercises = mutableListOf<PlannedExercise>()
        for ((index, item) in exercisesJson.items.withIndex()) {
            val obj = item as? JsonValue.Obj ?: return null
            val exerciseId = obj.int("exerciseId") ?: return null
            if (ExerciseData.getById(exerciseId) == null || !seen.add(exerciseId)) return null
            val sets = obj.int("sets") ?: return null
            val reps = obj.int("reps") ?: return null
            val restSeconds = obj.int("restSeconds") ?: return null
            if (sets !in 1..6 || reps !in 1..30 || restSeconds !in 15..300) return null
            val dayLabel = (obj.map["dayLabel"] as? JsonValue.Str)?.text ?: return null
            if (dayLabel !in allowed) return null
            val orderIndex = if (obj.map.containsKey("orderIndex")) {
                obj.int("orderIndex") ?: return null
            } else {
                index
            }
            if (orderIndex < 0) return null
            exercises.add(
                PlannedExercise(
                    exerciseId = exerciseId,
                    sets = sets,
                    reps = reps,
                    restSeconds = restSeconds,
                    orderIndex = orderIndex,
                    dayLabel = dayLabel
                )
            )
        }

        val name = (root.map["name"] as? JsonValue.Str)?.text?.trim().orEmpty()
            .ifBlank { defaultName(mode, bodyType) }
        val description = (root.map["description"] as? JsonValue.Str)?.text?.trim().orEmpty()
            .ifBlank { defaultDescription(mode, bodyType) }
        return RoutinePlan(
            name = name,
            description = description,
            isGenerated = true,
            exercises = exercises
        )
    }

    private fun JsonValue.Obj.int(key: String): Int? = (map[key] as? JsonValue.IntNum)?.number

    private fun unwrap(raw: String): String {
        var text = raw.trim()
        if (!text.startsWith("```")) return text
        text = text.removePrefix("```").trim()
        if (text.startsWith("json")) text = text.removePrefix("json").trim()
        if (text.endsWith("```")) text = text.removeSuffix("```").trim()
        return text
    }

    private fun defaultName(mode: RoutinePlanMode, bodyType: String): String {
        val biotipo = bodyType.ifBlank { "tu biotipo" }
        return when (mode) {
            RoutinePlanMode.FullBody -> "Cuerpo completo $biotipo"
            RoutinePlanMode.Weekly -> "Plan semanal $biotipo"
            RoutinePlanMode.Suggested -> "Rutina sugerida $biotipo"
        }
    }

    private fun defaultDescription(mode: RoutinePlanMode, bodyType: String): String {
        val biotipo = bodyType.ifBlank { "tu biotipo" }
        return when (mode) {
            RoutinePlanMode.FullBody -> "Rutina de cuerpo completo generada para biotipo $biotipo."
            RoutinePlanMode.Weekly -> "Plan semanal de empuje, tracción y piernas para biotipo $biotipo."
            RoutinePlanMode.Suggested -> "Rutina sugerida de un día para biotipo $biotipo."
        }
    }
}
