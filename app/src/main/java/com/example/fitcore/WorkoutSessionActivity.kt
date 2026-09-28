package com.example.fitcore

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fitcore.ExerciseData
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.db.RoutineExerciseEntity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class WorkoutSessionActivity : AppCompatActivity() {

    private lateinit var exercises: List<RoutineExerciseEntity>
    private var currentIndex = 0
    private var routineId: Long = 0

    private lateinit var tvName: TextView
    private lateinit var tvSets: TextView
    private lateinit var tvReps: TextView
    private lateinit var ivGif: ImageView
    private lateinit var progress: LinearProgressIndicator
    private lateinit var btnNext: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_workout_session)

        routineId = intent.getLongExtra("routineId", 0)
        val selectedDay = intent.getStringExtra("selectedDay") ?: "Todo"

        tvName = findViewById(R.id.tvExerciseName)
        tvSets = findViewById(R.id.tvSets)
        tvReps = findViewById(R.id.tvReps)
        ivGif = findViewById(R.id.ivExerciseGif)
        progress = findViewById(R.id.sessionProgress)
        btnNext = findViewById(R.id.btnNext)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarWorkout)
        toolbar.title = if (selectedDay != "Todo") "Entrenamiento: $selectedDay" else "Sesión de Entrenamiento"
        toolbar.setNavigationOnClickListener { finish() }

        loadExercises(selectedDay)

        btnNext.setOnClickListener {
            if (currentIndex < exercises.size - 1) {
                currentIndex++
                showExercise()
            } else {
                finishSession()
            }
        }
    }

    private fun loadExercises(selectedDay: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val routine = db.routineDao().getRoutineById(routineId)
            
            if (routine != null) {
                val allSorted = routine.exercises.sortedBy { it.orderIndex }
                
                // Filtramos por día si se ha seleccionado uno específico
                exercises = if (selectedDay == "Todo") {
                    allSorted
                } else {
                    allSorted.filter { it.dayLabel == selectedDay }
                }

                withContext(Dispatchers.Main) {
                    if (exercises.isNotEmpty()) {
                        showExercise()
                    } else {
                        Toast.makeText(this@WorkoutSessionActivity, "No hay ejercicios para hoy", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    private fun showExercise() {
        val current = exercises[currentIndex]
        val exerciseData = ExerciseData.getById(current.exerciseId)
        
        tvName.text = exerciseData?.name ?: "Ejercicio"
        tvSets.text = current.sets.toString()
        tvReps.text = current.reps.toString()
        
        Glide.with(this)
            .load(exerciseData?.imageUrl)
            .placeholder(R.drawable.placeholder_exercise)
            .into(ivGif)

        val p = ((currentIndex + 1).toFloat() / exercises.size * 100).toInt()
        progress.setProgress(p, true)

        if (currentIndex == exercises.size - 1) {
            btnNext.text = "FINALIZAR ENTRENAMIENTO"
        } else {
            btnNext.text = "SIGUIENTE EJERCICIO"
        }
    }

    private fun finishSession() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(applicationContext)
            val user = db.routineDao().getLoggedInUser()
            if (user != null) {
                val now = System.currentTimeMillis()
                val newXp = user.xp + 50
                db.routineDao().saveUserProfile(
                    user.copy(
                        xp = newXp,
                        level = (newXp / 200) + 1,
                        streakDays = nextStreakDays(user.lastWorkoutTimestamp, now, user.streakDays),
                        lastWorkoutTimestamp = now,
                        workoutsCompleted = user.workoutsCompleted + 1
                    )
                )
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@WorkoutSessionActivity, "¡Entrenamiento completado! +50 XP", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun nextStreakDays(lastWorkoutTimestamp: Long, now: Long, currentStreak: Int): Int {
        if (lastWorkoutTimestamp <= 0L) return 1
        val last = Calendar.getInstance().apply { timeInMillis = lastWorkoutTimestamp }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        if (isSameCalendarDay(last, today)) return currentStreak
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return if (isSameCalendarDay(last, yesterday)) currentStreak + 1 else 1
    }

    private fun isSameCalendarDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
}
