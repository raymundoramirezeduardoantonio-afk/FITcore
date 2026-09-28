package com.example.fitcore.agent

import com.example.fitcore.Exercise
import com.example.fitcore.ExerciseData
import com.example.fitcore.db.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRoutineAgentTest {

    private val agent = LocalRoutineAgent()
    private val catalogIds = ExerciseData.getAll().map { it.id }.toSet()

    @Test
    fun fullBody_ectomorfoSinImc_saleDelCatalogo() = runBlocking {
        val plan = agent.plan(user("Ectomorfo", height = 0f, weight = 0f), RoutinePlanMode.FullBody)

        assertTrue(plan.isGenerated)
        assertTrue(plan.name.contains("Ectomorfo"))
        assertTrue(plan.description.contains("Ectomorfo"))
        assertEquals(6, plan.exercises.size)
        val groups = listOf("Pecho", "Espalda", "Piernas", "Hombros", "Brazos", "Core")
        assertEquals(groups, plan.exercises.map { groupOf(it.exerciseId) })
        val firstOfEachGroup = groups.map { group ->
            ExerciseData.getByMuscleGroup(group).minBy { it.id }.id
        }
        assertEquals(firstOfEachGroup, plan.exercises.map { it.exerciseId })
        assertTrue(plan.exercises.all { it.dayLabel == "Todo" })
        assertTrue(plan.exercises.all { it.sets == 3 && it.reps == 8 && it.restSeconds == 120 })
        assertEquals((0 until 6).toList(), plan.exercises.map { it.orderIndex })
        assertUniqueCatalogIds(plan)
    }

    @Test
    fun volumen_respetaBiotipoEImc() = runBlocking {
        val meso = agent.plan(user("Mesomorfo", 170f, 70f), RoutinePlanMode.FullBody)
        assertTrue(meso.exercises.all { it.sets == 4 && it.reps == 10 && it.restSeconds == 90 })

        val endo = agent.plan(user("Endomorfo", 0f, 0f), RoutinePlanMode.FullBody)
        assertTrue(endo.exercises.all { it.sets == 4 && it.reps == 15 && it.restSeconds == 45 })

        val height = 150f
        val heightM = height / 100f
        val bajo = agent.plan(
            user("Ectomorfo", height, 18.4f * heightM * heightM),
            RoutinePlanMode.FullBody
        )
        assertTrue(bajo.exercises.all { it.reps == 6 && it.restSeconds == 150 })
        assertEquals("Barra", ExerciseData.getById(bajo.exercises.first().exerciseId)!!.equipment)

        val normal = agent.plan(
            user("Ectomorfo", height, 18.5f * heightM * heightM),
            RoutinePlanMode.FullBody
        )
        assertTrue(normal.exercises.all { it.reps == 8 && it.restSeconds == 120 })

        val alto = agent.plan(
            user("Ectomorfo", height, 25f * heightM * heightM),
            RoutinePlanMode.FullBody
        )
        assertTrue(alto.exercises.all { it.reps == 12 && it.restSeconds == 105 })
        assertTrue(
            ExerciseData.getById(alto.exercises.first().exerciseId)!!.equipment == "Peso corporal" ||
                ExerciseData.getById(alto.exercises.first().exerciseId)!!.equipment == "Máquina"
        )

        val invalido = agent.plan(user("Ectomorfo", 99f, 80f), RoutinePlanMode.FullBody)
        assertTrue(invalido.exercises.all { it.reps == 8 && it.restSeconds == 120 })
    }

    @Test
    fun weekly_reparteDiasSinRepetirIds() = runBlocking {
        val plan = agent.plan(user("Mesomorfo", 170f, 70f), RoutinePlanMode.Weekly)

        assertTrue(plan.isGenerated)
        assertEquals(18, plan.exercises.size)
        assertEquals(6, plan.exercises.count { it.dayLabel == "Lunes" })
        assertEquals(6, plan.exercises.count { it.dayLabel == "Miércoles" })
        assertEquals(6, plan.exercises.count { it.dayLabel == "Viernes" })
        assertEquals(
            listOf("Pecho", "Pecho", "Hombros", "Hombros", "Brazos", "Brazos"),
            plan.exercises.filter { it.dayLabel == "Lunes" }.map { groupOf(it.exerciseId) }
        )
        assertEquals(
            listOf("Espalda", "Espalda", "Espalda", "Brazos", "Brazos", "Brazos"),
            plan.exercises.filter { it.dayLabel == "Miércoles" }.map { groupOf(it.exerciseId) }
        )
        assertEquals(
            listOf("Piernas", "Piernas", "Piernas", "Piernas", "Core", "Core"),
            plan.exercises.filter { it.dayLabel == "Viernes" }.map { groupOf(it.exerciseId) }
        )
        assertTrue(plan.exercises.filter { it.dayLabel == "Lunes" }.takeLast(2).all { isPushArm(it.exerciseId) })
        assertTrue(plan.exercises.filter { it.dayLabel == "Miércoles" }.takeLast(3).all { isPullArm(it.exerciseId) })
        assertUniqueCatalogIds(plan)
    }

    @Test
    fun weekly_sobrepeso_priorizaPesoCorporalOMaquina() = runBlocking {
        val height = 170f
        val heightM = height / 100f
        val plan = agent.plan(
            user("Endomorfo", height, 30f * heightM * heightM),
            RoutinePlanMode.Weekly
        )
        val pecho = plan.exercises.filter { groupOf(it.exerciseId) == "Pecho" }
        assertEquals(2, pecho.size)
        assertTrue(pecho.all { preferredEquipment(it.exerciseId) })

        val espalda = plan.exercises.filter { groupOf(it.exerciseId) == "Espalda" }
        assertTrue(espalda.all { ExerciseData.getById(it.exerciseId)!!.equipment == "Polea" })
        assertTrue(plan.exercises.all { it.reps == 19 && it.restSeconds == 30 })
    }

    @Test
    fun suggested_cuentaSegunBiotipoYMarcaGenerada() = runBlocking {
        val ecto = agent.plan(user("Ectomorfo", 180f, 50f), RoutinePlanMode.Suggested)
        val meso = agent.plan(user("Mesomorfo", 170f, 70f), RoutinePlanMode.Suggested)
        val endo = agent.plan(user("Endomorfo", 160f, 90f), RoutinePlanMode.Suggested)

        assertEquals(4, ecto.exercises.size)
        assertEquals(5, meso.exercises.size)
        assertEquals(6, endo.exercises.size)
        listOf(ecto, meso, endo).forEach { plan ->
            assertTrue(plan.isGenerated)
            assertTrue(plan.exercises.all { it.dayLabel == "Todo" })
            assertUniqueCatalogIds(plan)
        }
        assertTrue(ecto.name.contains("Ectomorfo"))
        assertTrue(meso.description.contains("Mesomorfo"))
        assertTrue(endo.description.contains("Endomorfo"))
    }

    private fun user(bodyType: String, height: Float, weight: Float): UserEntity {
        return UserEntity(id = 4, bodyType = bodyType, height = height, weight = weight, isLoggedIn = true)
    }

    private fun groupOf(exerciseId: Int): String {
        return ExerciseData.getById(exerciseId)!!.muscleGroup
    }

    private fun preferredEquipment(exerciseId: Int): Boolean {
        val equipment = ExerciseData.getById(exerciseId)!!.equipment
        return equipment == "Peso corporal" || equipment == "Máquina"
    }

    private fun isPushArm(exerciseId: Int): Boolean {
        val name = normalize(ExerciseData.getById(exerciseId)!!.name)
        return name.contains("tricep") || name.contains("frances") || name.contains("patada") ||
            name.contains("agarre cerrado") || name.contains("fondo")
    }

    private fun isPullArm(exerciseId: Int): Boolean {
        val name = normalize(ExerciseData.getById(exerciseId)!!.name)
        return name.contains("bicep") || name.contains("curl") || name.contains("martillo")
    }

    private fun normalize(value: String): String {
        return value.lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
    }

    private fun assertUniqueCatalogIds(plan: RoutinePlan) {
        val ids = plan.exercises.map { it.exerciseId }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it in catalogIds })
        assertTrue(plan.exercises.all { ExerciseData.getById(it.exerciseId) is Exercise })
    }
}
