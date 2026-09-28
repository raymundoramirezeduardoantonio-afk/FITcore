package com.example.fitcore.ui.routines

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitcore.Exercise
import com.example.fitcore.ExerciseAdapter
import com.example.fitcore.ExerciseData
import com.example.fitcore.R
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.db.RoutineExerciseEntity
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class AddExerciseToRoutineActivity : AppCompatActivity() {

    private var routineId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_add_exercise_to_routine)

        routineId = intent.getLongExtra("routineId", 0)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val rvExercises = findViewById<RecyclerView>(R.id.rvExercises)
        val etSearch = findViewById<EditText>(R.id.etSearch)

        val adapter = ExerciseAdapter { exercise ->
            addExerciseToRoutine(exercise)
        }
        rvExercises.layoutManager = LinearLayoutManager(this)
        rvExercises.adapter = adapter
        adapter.submitList(ExerciseData.getAll())

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.submitList(ExerciseData.search(s?.toString() ?: ""))
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun addExerciseToRoutine(exercise: Exercise) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val routine = db.routineDao().getRoutineById(routineId) ?: return@launch
            val count = routine.exercises.size
            db.routineDao().insertRoutineExercises(
                listOf(
                    RoutineExerciseEntity(
                        routineId = routineId,
                        exerciseId = exercise.id,
                        sets = 3,
                        reps = 10,
                        restSeconds = 60,
                        orderIndex = count
                    )
                )
            )
            Toast.makeText(this@AddExerciseToRoutineActivity,
                "'${exercise.name}' agregado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
