package com.example.fitcore

import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.db.RoutineExerciseEntity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first

class ExerciseDetailActivity : AppCompatActivity() {

    private var exercise: Exercise? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_exercise_detail)

        exercise = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("exercise", Exercise::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("exercise") as? Exercise
        }
        if (exercise == null) { finish(); return }

        val ex = exercise!!

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val ivGif = findViewById<ImageView>(R.id.ivExerciseGif)
        val tvName = findViewById<TextView>(R.id.tvDetailName)
        val tvMuscle = findViewById<TextView>(R.id.tvDetailMuscle)
        val tvEquipment = findViewById<TextView>(R.id.tvDetailEquipment)
        val tvDifficulty = findViewById<TextView>(R.id.tvDetailDifficulty)
        val tvDifficultyInfo = findViewById<TextView>(R.id.tvDetailDifficultyInfo)
        val tvInstructions = findViewById<TextView>(R.id.tvDetailInstructions)
        val btnAddRoutine = findViewById<MaterialButton>(R.id.btnAddToRoutine)

        val color = getMuscleColor(ex.muscleGroup)
        tvName.text = ex.name
        tvMuscle.text = "Grupo muscular: ${ex.muscleGroup}"
        tvMuscle.setTextColor(color)
        tvEquipment.text = ex.equipment

        val diffColor = getDifficultyColor(ex.difficulty)
        tvDifficulty.text = ex.difficulty
        tvDifficulty.setTextColor(diffColor)
        val bgRes = when (ex.difficulty) {
            "Principiante" -> R.drawable.bg_badge_beginner
            "Intermedio" -> R.drawable.bg_badge_intermediate
            "Avanzado" -> R.drawable.bg_badge_advanced
            else -> R.drawable.bg_badge_beginner
        }
        tvDifficulty.setBackgroundResource(bgRes)

        tvDifficultyInfo.text = ex.difficulty
        tvDifficultyInfo.setTextColor(diffColor)

        tvInstructions.text = ex.instructions

        Glide.with(this)
            .load(ex.imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .placeholder(R.drawable.placeholder_exercise)
            .error(R.drawable.placeholder_exercise)
            .into(ivGif)

        btnAddRoutine.setOnClickListener {
            showAddToRoutineDialog(ex)
        }
    }
    private fun showAddToRoutineDialog(exercise: Exercise) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)

            // CORRECCIÓN: Añade .first() al final para obtener la lista del Flow
            val routines = db.routineDao().getAllRoutines().first()

            withContext(Dispatchers.Main) {
                if (routines.isEmpty()) {
                    Toast.makeText(this@ExerciseDetailActivity,
                        "Primero crea una rutina desde Mis Rutinas", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val routineNames = routines.map { it.routine.name }.toTypedArray()

                android.app.AlertDialog.Builder(this@ExerciseDetailActivity, R.style.Theme_FITCORE)
                    .setTitle("Agregar a rutina")
                    .setItems(routineNames) { _, which ->
                        val routine = routines[which]
                        lifecycleScope.launch(Dispatchers.IO) {
                            val count = routine.exercises.size
                            db.routineDao().insertRoutineExercises(
                                listOf(
                                    RoutineExerciseEntity(
                                        routineId = routine.routine.id,
                                        exerciseId = exercise.id,
                                        sets = 3,
                                        reps = 10,
                                        restSeconds = 60,
                                        orderIndex = count
                                    )
                                )
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ExerciseDetailActivity,
                                    "'${exercise.name}' agregado a '${routine.routine.name}'",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    private fun getMuscleColor(group: String): Int = when (group) {
        "Pecho" -> ContextCompat.getColor(this, R.color.tag_chest)
        "Espalda" -> ContextCompat.getColor(this, R.color.tag_back)
        "Piernas" -> ContextCompat.getColor(this, R.color.tag_legs)
        "Hombros" -> ContextCompat.getColor(this, R.color.tag_shoulders)
        "Brazos" -> ContextCompat.getColor(this, R.color.tag_arms)
        "Core" -> ContextCompat.getColor(this, R.color.tag_core)
        else -> ContextCompat.getColor(this, R.color.neon_cyan)
    }

    private fun getDifficultyColor(diff: String): Int = when (diff) {
        "Principiante" -> ContextCompat.getColor(this, R.color.difficulty_beginner)
        "Intermedio" -> ContextCompat.getColor(this, R.color.difficulty_intermediate)
        "Avanzado" -> ContextCompat.getColor(this, R.color.difficulty_advanced)
        else -> ContextCompat.getColor(this, R.color.text_secondary)
    }
}
