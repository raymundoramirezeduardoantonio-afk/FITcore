package com.example.fitcore.ui.profile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitcore.LoginActivity
import com.example.fitcore.R
import com.example.fitcore.agent.RoutineAgents
import com.example.fitcore.agent.RoutinePlanMode
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.db.RoutineEntity
import com.example.fitcore.db.RoutineExerciseEntity
import com.example.fitcore.db.UserEntity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var etHeight: EditText
    private lateinit var etWeight: EditText
    private lateinit var toggleBodyType: MaterialButtonToggleGroup
    private lateinit var tvIMCValue: TextView
    private lateinit var tvIMCCategory: TextView
    private lateinit var tvUserName: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etHeight = view.findViewById(R.id.etHeight)
        etWeight = view.findViewById(R.id.etWeight)
        toggleBodyType = view.findViewById(R.id.toggleBodyType)
        tvIMCValue = view.findViewById(R.id.tvIMCValue)
        tvIMCCategory = view.findViewById(R.id.tvIMCCategory)
        tvUserName = view.findViewById(R.id.tvUserName)

        setupDynamicIMC()
        loadProfile()

        view.findViewById<MaterialButton>(R.id.btnSaveProfile).setOnClickListener {
            saveProfile()
        }

        view.findViewById<MaterialButton>(R.id.btnGenerateRoutine).setOnClickListener {
            generateSuggestedRoutine()
        }

        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            logout()
        }
    }

    private fun setupDynamicIMC() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateAndDisplayIMC()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etHeight.addTextChangedListener(watcher)
        etWeight.addTextChangedListener(watcher)
    }

    private fun calculateAndDisplayIMC() {
        val h = etHeight.text.toString().toFloatOrNull() ?: 0f
        val w = etWeight.text.toString().toFloatOrNull() ?: 0f
        updateIMCDisplay(h, w)
    }

    private fun logout() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.routineDao().logoutAll()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val profile = db.routineDao().getLoggedInUser()
            profile?.let {
                tvUserName.text = "Hola, ${it.name}"
                etHeight.setText(it.height.toInt().toString())
                etWeight.setText(it.weight.toString())
                when (it.bodyType) {
                    "Ectomorfo" -> toggleBodyType.check(R.id.btnEcto)
                    "Mesomorfo" -> toggleBodyType.check(R.id.btnMeso)
                    "Endomorfo" -> toggleBodyType.check(R.id.btnEndo)
                }
                updateIMCDisplay(it.height, it.weight)
            }
        }
    }

    private fun saveProfile() {
        val heightStr = etHeight.text.toString()
        val weightStr = etWeight.text.toString()

        if (heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(context, "Por favor ingresa altura y peso", Toast.LENGTH_SHORT).show()
            return
        }

        val height = heightStr.toFloat()
        val weight = weightStr.toFloat()
        
        val bodyType = when (toggleBodyType.checkedButtonId) {
            R.id.btnEcto -> "Ectomorfo"
            R.id.btnMeso -> "Mesomorfo"
            R.id.btnEndo -> "Endomorfo"
            else -> ""
        }

        if (bodyType.isEmpty()) {
            Toast.makeText(context, "Selecciona un tipo de cuerpo", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val current = db.routineDao().getLoggedInUser()
            val updated = current?.copy(height = height, weight = weight, bodyType = bodyType) 
                ?: UserEntity(height = height, weight = weight, bodyType = bodyType, isLoggedIn = true)
            
            db.routineDao().saveUserProfile(updated)
            Toast.makeText(context, "Perfil guardado correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateSuggestedRoutine() {
        val bodyType = when (toggleBodyType.checkedButtonId) {
            R.id.btnEcto -> "Ectomorfo"
            R.id.btnMeso -> "Mesomorfo"
            R.id.btnEndo -> "Endomorfo"
            else -> ""
        }

        if (bodyType.isEmpty()) return

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val user = db.routineDao().getLoggedInUser() ?: return@launch
            val height = etHeight.text.toString().toFloatOrNull() ?: user.height
            val weight = etWeight.text.toString().toFloatOrNull() ?: user.weight
            val snapshot = user.copy(bodyType = bodyType, height = height, weight = weight)
            val plan = RoutineAgents.create().plan(snapshot, RoutinePlanMode.Suggested)

            val routineId = db.routineDao().insertRoutine(
                RoutineEntity(
                    userId = user.id,
                    name = plan.name,
                    description = plan.description,
                    isGenerated = true
                )
            )

            val exercises = plan.exercises.map { planned ->
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = planned.exerciseId,
                    sets = planned.sets,
                    reps = planned.reps,
                    restSeconds = planned.restSeconds,
                    orderIndex = planned.orderIndex,
                    dayLabel = planned.dayLabel
                )
            }
            db.routineDao().insertRoutineExercises(exercises)

            Toast.makeText(context, "¡Rutina '${plan.name}' creada!", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateIMCDisplay(heightCm: Float, weightKg: Float) {
        if (heightCm < 100 || weightKg < 20) {
            tvIMCValue.text = "--"
            tvIMCCategory.text = "Ingresa datos válidos"
            tvIMCCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            return
        }
        
        val heightM = heightCm / 100
        val imc = weightKg / (heightM * heightM)
        
        tvIMCValue.text = String.format("%.1f", imc)
        
        val (category, colorRes) = when {
            imc < 18.5 -> "Bajo peso (Necesitas más calorías)" to R.color.difficulty_intermediate
            imc < 25.0 -> "Peso normal (¡Excelente estado!)" to R.color.difficulty_beginner
            imc < 30.0 -> "Sobrepeso (Ideal: Recomposición)" to R.color.difficulty_intermediate
            else -> "Obesidad (Prioriza tu salud cardiovascular)" to R.color.difficulty_advanced
        }
        
        tvIMCCategory.text = category
        tvIMCCategory.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }
}
