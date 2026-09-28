package com.example.fitcore.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutinePlanParserTest {

    @Test
    fun jsonValido_aceptaPlan() {
        val json = """
            {
              "name": "Fuerza",
              "description": "Sesión corta",
              "exercises": [
                {"exerciseId": 1, "sets": 3, "reps": 8, "restSeconds": 90, "orderIndex": 0, "dayLabel": "Todo"},
                {"exerciseId": 6, "sets": 4, "reps": 10, "restSeconds": 60, "orderIndex": 1, "dayLabel": "Todo"}
              ]
            }
        """.trimIndent()

        val plan = RoutinePlanParser.parse(json, RoutinePlanMode.FullBody)

        assertNotNull(plan)
        plan!!
        assertTrue(plan.isGenerated)
        assertEquals("Fuerza", plan.name)
        assertEquals("Sesión corta", plan.description)
        assertEquals(listOf(1, 6), plan.exercises.map { it.exerciseId })
        assertEquals(listOf(3, 4), plan.exercises.map { it.sets })
        assertEquals(listOf(8, 10), plan.exercises.map { it.reps })
        assertEquals(listOf(90, 60), plan.exercises.map { it.restSeconds })
        assertTrue(plan.exercises.all { it.dayLabel == "Todo" })
    }

    @Test
    fun weekly_aceptaMiercoles() {
        val plan = RoutinePlanParser.parse(
            exercise(dayLabel = "Miércoles", id = 6),
            RoutinePlanMode.Weekly
        )

        assertNotNull(plan)
        assertEquals("Miércoles", plan!!.exercises.single().dayLabel)
    }

    @Test
    fun idInventado_rechaza() {
        assertNull(RoutinePlanParser.parse(exercise(id = 999999), RoutinePlanMode.FullBody))
    }

    @Test
    fun idDuplicado_rechaza() {
        val json = """
            {"name":"Fuerza","description":"Sesión","exercises":[
              {"exerciseId":1,"sets":3,"reps":8,"restSeconds":90,"orderIndex":0,"dayLabel":"Todo"},
              {"exerciseId":1,"sets":3,"reps":8,"restSeconds":90,"orderIndex":1,"dayLabel":"Todo"}
            ]}
        """.trimIndent()

        assertNull(RoutinePlanParser.parse(json, RoutinePlanMode.FullBody))
    }

    @Test
    fun dayLabelIlegal_rechaza() {
        assertNull(
            RoutinePlanParser.parse(exercise(dayLabel = "Lunes"), RoutinePlanMode.FullBody)
        )
        assertNull(
            RoutinePlanParser.parse(exercise(dayLabel = "Todo"), RoutinePlanMode.Weekly)
        )
        assertNull(
            RoutinePlanParser.parse(exercise(dayLabel = "Sábado"), RoutinePlanMode.Suggested)
        )
    }

    @Test
    fun fueraDeRango_rechaza() {
        assertNull(RoutinePlanParser.parse(exercise(sets = 0), RoutinePlanMode.FullBody))
        assertNull(RoutinePlanParser.parse(exercise(sets = 7), RoutinePlanMode.FullBody))
        assertNull(RoutinePlanParser.parse(exercise(reps = 0), RoutinePlanMode.FullBody))
        assertNull(RoutinePlanParser.parse(exercise(reps = 31), RoutinePlanMode.FullBody))
        assertNull(RoutinePlanParser.parse(exercise(rest = 14), RoutinePlanMode.FullBody))
        assertNull(RoutinePlanParser.parse(exercise(rest = 301), RoutinePlanMode.FullBody))
        assertNotNull(RoutinePlanParser.parse(exercise(sets = 1, reps = 1, rest = 15), RoutinePlanMode.FullBody))
        assertNotNull(RoutinePlanParser.parse(exercise(sets = 6, reps = 30, rest = 300), RoutinePlanMode.FullBody))
    }

    private fun exercise(
        dayLabel: String = "Todo",
        id: Int = 1,
        sets: Int = 3,
        reps: Int = 8,
        rest: Int = 90
    ): String {
        return """
            {"name":"Fuerza","description":"Sesión","exercises":[
              {"exerciseId":$id,"sets":$sets,"reps":$reps,"restSeconds":$rest,"orderIndex":0,"dayLabel":"$dayLabel"}
            ]}
        """.trimIndent()
    }
}
