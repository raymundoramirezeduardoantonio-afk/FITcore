package com.example.fitcore.agent

import com.example.fitcore.ExerciseData
import com.example.fitcore.db.UserEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class OpenAiRoutineAgent(
    private val apiKey: String,
    private val model: String,
    private val fallback: RoutineAgent
) : RoutineAgent {

    override suspend fun plan(user: UserEntity, mode: RoutinePlanMode): RoutinePlan {
        if (apiKey.isBlank() || apiKey.any { it == '\n' || it == '\r' }) {
            return fallback.plan(user, mode)
        }
        return withContext(Dispatchers.IO) {
            try {
                val content = RoutinePlanParser.extractAssistantContent(request(user, mode))
                val parsed = content?.let { RoutinePlanParser.parse(it, mode, user.bodyType) }
                parsed ?: fallback.plan(user, mode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                fallback.plan(user, mode)
            }
        }
    }

    private fun request(user: UserEntity, mode: RoutinePlanMode): String {
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            val payload = requestBody(user, mode).toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(payload) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw java.io.IOException("OpenAI HTTP $code")
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun requestBody(user: UserEntity, mode: RoutinePlanMode): String {
        val resolvedModel = model.ifBlank { DEFAULT_MODEL }
        return buildString {
            append("{\"model\":")
            append(JsonText.encodeString(resolvedModel))
            append(",\"temperature\":0.2,\"response_format\":{\"type\":\"json_object\"},\"messages\":[")
            append("{\"role\":\"system\",\"content\":")
            append(JsonText.encodeString(SYSTEM_PROMPT))
            append("},{\"role\":\"user\",\"content\":")
            append(JsonText.encodeString(userPrompt(user, mode)))
            append("}]}")
        }
    }

    private fun userPrompt(user: UserEntity, mode: RoutinePlanMode): String {
        val catalog = ExerciseData.getAll().joinToString(separator = "\n") { exercise ->
            listOf(
                exercise.id.toString(),
                exercise.name,
                exercise.muscleGroup,
                exercise.equipment,
                exercise.difficulty
            ).joinToString("|")
        }
        val labels = RoutinePlanParser.allowedDayLabels(mode).joinToString(", ")
        return """
            Diseña una rutina de gimnasio.
            Biotipo: ${user.bodyType.ifBlank { "no indicado" }}
            Altura_cm: ${user.height}
            Peso_kg: ${user.weight}
            IMC: ${bmiText(user)}
            Modo: ${mode.name}
            dayLabel permitidos para este modo: $labels

            ${contract(mode)}

            Responde solo un objeto JSON con esta forma:
            {"name":"string","description":"string","exercises":[{"exerciseId":1,"sets":3,"reps":8,"restSeconds":90,"orderIndex":0,"dayLabel":"Todo"}]}

            Reglas:
            - exerciseId tiene que ser un id del catálogo. No inventes ids.
            - No repitas exerciseId.
            - sets de 1 a 6, reps de 1 a 30, restSeconds de 15 a 300.
            - dayLabel solo puede ser uno de los permitidos para este modo.
            - Incluye al menos un ejercicio.
            - Propón series, repeticiones y descanso según el biotipo y el IMC cuando el IMC aplique.

            Catálogo (id|name|muscleGroup|equipment|difficulty):
            $catalog
        """.trimIndent()
    }

    private fun contract(mode: RoutinePlanMode): String = when (mode) {
        RoutinePlanMode.FullBody ->
            "FullBody: un solo día con dayLabel \"Todo\". Orientación: 6 ejercicios, uno por grupo (Pecho, Espalda, Piernas, Hombros, Brazos, Core)."
        RoutinePlanMode.Weekly ->
            "Weekly: \"Lunes\" es empuje (Pecho, Hombros, Brazos), \"Miércoles\" es tracción (Espalda, Brazos) y \"Viernes\" es Piernas y Core. Orientación: unos 6 ejercicios por día."
        RoutinePlanMode.Suggested ->
            "Suggested: un solo día con dayLabel \"Todo\". Orientación: de 4 a 6 ejercicios según el biotipo (Ectomorfo cerca de 4, Mesomorfo cerca de 5, Endomorfo cerca de 6)."
    }

    private fun bmiText(user: UserEntity): String {
        if (user.height < 100f || user.weight < 20f) return "no aplica"
        val heightM = user.height / 100f
        val imc = user.weight / (heightM * heightM)
        return String.format(Locale.US, "%.1f", imc)
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-4.1-mini"
        private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
        private const val TIMEOUT_MS = 20_000
        private const val SYSTEM_PROMPT =
            "Eres un preparador físico. Respondes únicamente con un objeto JSON, sin markdown, y solo usas ids del catálogo."
    }
}
