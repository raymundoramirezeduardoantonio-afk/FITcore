package com.example.fitcore.agent

import com.example.fitcore.Exercise
import com.example.fitcore.ExerciseData
import com.example.fitcore.db.UserEntity
import java.util.Locale

/**
 * Planificador local. Elige ejercicios reales de [ExerciseData] según biotipo e IMC.
 * No usa red ni identificadores fijos.
 */
class LocalRoutineAgent : RoutineAgent {

    override suspend fun plan(user: UserEntity, mode: RoutinePlanMode): RoutinePlan {
        val band = bmiBand(user)
        val volume = volumeFor(user.bodyType, band)
        val used = mutableSetOf<Int>()
        val exercises = mutableListOf<PlannedExercise>()
        var order = 0

        for (slot in slotsFor(mode, user.bodyType)) {
            val picked = pick(slot, used, band)
            for (exercise in picked) {
                exercises.add(
                    PlannedExercise(
                        exerciseId = exercise.id,
                        sets = volume.sets,
                        reps = volume.reps,
                        restSeconds = volume.restSeconds,
                        orderIndex = order,
                        dayLabel = slot.dayLabel
                    )
                )
                order++
            }
        }

        return RoutinePlan(
            name = nameFor(mode, user.bodyType),
            description = descriptionFor(mode, user.bodyType, band),
            isGenerated = true,
            exercises = exercises
        )
    }

    private fun bmiBand(user: UserEntity): BmiBand {
        if (user.height < 100f || user.weight < 20f) return BmiBand.Ignored
        val heightM = user.height / 100f
        val imc = user.weight / (heightM * heightM)
        return when {
            imc < 18.5f -> BmiBand.Underweight
            imc < 25f -> BmiBand.Normal
            else -> BmiBand.Overweight
        }
    }

    private fun volumeFor(bodyType: String, band: BmiBand): Volume {
        val base = when (bodyType) {
            "Ectomorfo" -> Volume(sets = 3, reps = 8, restSeconds = 120)
            "Mesomorfo" -> Volume(sets = 4, reps = 10, restSeconds = 90)
            else -> Volume(sets = 4, reps = 15, restSeconds = 45)
        }
        return when (band) {
            BmiBand.Underweight -> base.copy(
                reps = (base.reps - 2).coerceAtLeast(4),
                restSeconds = base.restSeconds + 30
            )
            BmiBand.Overweight -> base.copy(
                reps = base.reps + 4,
                restSeconds = (base.restSeconds - 15).coerceAtLeast(30)
            )
            BmiBand.Normal, BmiBand.Ignored -> base
        }
    }

    private fun slotsFor(mode: RoutinePlanMode, bodyType: String): List<DaySlot> {
        return when (mode) {
            RoutinePlanMode.FullBody -> listOf(
                DaySlot("Todo", "Pecho", 1),
                DaySlot("Todo", "Espalda", 1),
                DaySlot("Todo", "Piernas", 1),
                DaySlot("Todo", "Hombros", 1),
                DaySlot("Todo", "Brazos", 1),
                DaySlot("Todo", "Core", 1)
            )
            RoutinePlanMode.Weekly -> listOf(
                DaySlot("Lunes", "Pecho", 2),
                DaySlot("Lunes", "Hombros", 2),
                DaySlot("Lunes", "Brazos", 2, ArmBias.Push),
                DaySlot("Miércoles", "Espalda", 3),
                DaySlot("Miércoles", "Brazos", 3, ArmBias.Pull),
                DaySlot("Viernes", "Piernas", 4),
                DaySlot("Viernes", "Core", 2)
            )
            RoutinePlanMode.Suggested -> suggestedGroups(bodyType).map { group ->
                DaySlot("Todo", group, 1)
            }
        }
    }

    private fun suggestedGroups(bodyType: String): List<String> {
        return when (bodyType) {
            "Ectomorfo" -> listOf("Pecho", "Espalda", "Piernas", "Hombros")
            "Mesomorfo" -> listOf("Pecho", "Espalda", "Piernas", "Hombros", "Brazos")
            else -> listOf("Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Core")
        }
    }

    private fun pick(slot: DaySlot, used: MutableSet<Int>, band: BmiBand): List<Exercise> {
        if (slot.count <= 0) return emptyList()
        val pool = ExerciseData.getAll().filter { exercise ->
            exercise.muscleGroup.equals(slot.group, ignoreCase = true) && exercise.id !in used
        }
        if (pool.isEmpty()) return emptyList()

        val biased = if (slot.group.equals("Brazos", ignoreCase = true) && slot.armBias != ArmBias.Any) {
            pool.filter { matchesArm(it, slot.armBias) }
        } else {
            emptyList()
        }
        val primary = if (biased.isNotEmpty()) biased else pool
        val ordered = (primary.sortedWith(comparator(band)) + pool.sortedWith(comparator(band)))
            .distinctBy { it.id }

        val chosen = ordered.take(slot.count)
        chosen.forEach { used.add(it.id) }
        return chosen
    }

    private fun comparator(band: BmiBand): Comparator<Exercise> {
        return if (band == BmiBand.Underweight || band == BmiBand.Overweight) {
            compareBy<Exercise> { equipmentRank(it.equipment, band) }.thenBy { it.id }
        } else {
            compareBy { it.id }
        }
    }

    private fun equipmentRank(equipment: String, band: BmiBand): Int {
        val key = normalize(equipment)
        return when (band) {
            BmiBand.Underweight -> when (key) {
                "barra" -> 0
                "mancuernas" -> 1
                "peso corporal" -> 2
                "polea" -> 3
                "maquina" -> 4
                else -> 5
            }
            BmiBand.Overweight -> when (key) {
                "peso corporal", "maquina" -> 0
                "polea" -> 1
                "mancuernas" -> 2
                "barra" -> 3
                else -> 4
            }
            BmiBand.Normal, BmiBand.Ignored -> 0
        }
    }

    private fun matchesArm(exercise: Exercise, bias: ArmBias): Boolean {
        val name = normalize(exercise.name)
        return when (bias) {
            ArmBias.Push -> name.contains("tricep") ||
                name.contains("frances") ||
                name.contains("patada") ||
                name.contains("agarre cerrado") ||
                name.contains("fondo")
            ArmBias.Pull -> name.contains("bicep") ||
                name.contains("curl") ||
                name.contains("martillo")
            ArmBias.Any -> true
        }
    }

    private fun nameFor(mode: RoutinePlanMode, bodyType: String): String {
        val biotipo = bodyType.ifBlank { "tu biotipo" }
        return when (mode) {
            RoutinePlanMode.FullBody -> "Cuerpo completo $biotipo"
            RoutinePlanMode.Weekly -> "Plan semanal $biotipo"
            RoutinePlanMode.Suggested -> when (bodyType) {
                "Ectomorfo" -> "Volumen Ectomorfo"
                "Mesomorfo" -> "Potencia Mesomorfo"
                "Endomorfo" -> "Definición Endomorfo"
                else -> "Rutina sugerida $biotipo"
            }
        }
    }

    private fun descriptionFor(mode: RoutinePlanMode, bodyType: String, band: BmiBand): String {
        val biotipo = bodyType.ifBlank { "tu biotipo" }
        val focus = when (mode) {
            RoutinePlanMode.FullBody -> "Rutina de cuerpo completo generada para biotipo $biotipo."
            RoutinePlanMode.Weekly -> "Plan semanal de empuje, tracción y piernas para biotipo $biotipo."
            RoutinePlanMode.Suggested -> "Rutina sugerida de un día para biotipo $biotipo."
        }
        val imc = when (band) {
            BmiBand.Underweight -> " Prioriza compuestos y descansos más largos por IMC bajo."
            BmiBand.Normal -> " Volumen equilibrado para un IMC normal."
            BmiBand.Overweight -> " Prioriza peso corporal o máquinas y más repeticiones por IMC elevado."
            BmiBand.Ignored -> " Volumen según biotipo."
        }
        return focus + imc
    }

    private fun normalize(value: String): String {
        return value.lowercase(Locale.ROOT)
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ü", "u")
    }

    private data class Volume(val sets: Int, val reps: Int, val restSeconds: Int)

    private data class DaySlot(
        val dayLabel: String,
        val group: String,
        val count: Int,
        val armBias: ArmBias = ArmBias.Any
    )

    private enum class ArmBias { Any, Push, Pull }

    private enum class BmiBand { Ignored, Underweight, Normal, Overweight }
}
