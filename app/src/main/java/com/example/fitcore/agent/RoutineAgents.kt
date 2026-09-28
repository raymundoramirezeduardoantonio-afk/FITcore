package com.example.fitcore.agent

import com.example.fitcore.BuildConfig

object RoutineAgents {
    fun create(): RoutineAgent {
        return OpenAiRoutineAgent(
            apiKey = BuildConfig.OPENAI_API_KEY,
            model = BuildConfig.OPENAI_MODEL.ifBlank { OpenAiRoutineAgent.DEFAULT_MODEL },
            fallback = LocalRoutineAgent()
        )
    }
}
