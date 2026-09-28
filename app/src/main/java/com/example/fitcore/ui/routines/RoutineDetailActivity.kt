package com.example.fitcore.ui.routines

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitcore.Exercise
import com.example.fitcore.ExerciseData
import com.example.fitcore.ExerciseDetailActivity
import com.example.fitcore.R
import com.example.fitcore.WorkoutSessionActivity
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.db.RoutineExerciseEntity
import com.example.fitcore.db.RoutineWithExercises
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class RoutineDetailActivity : AppCompatActivity() {

    private var routineId: Long = 0
    private lateinit var adapter: RoutineExerciseAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvTotal: TextView
    private lateinit var rvExercises: RecyclerView
    private lateinit var chipGroupDays: ChipGroup
    private lateinit var scrollDays: View
    private lateinit var btnStart: MaterialButton
    
    private var allExercisesList = listOf<RoutineExerciseWithDetails>()
    private var currentFilter = "Todo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_routine_detail)

        routineId = intent.getLongExtra("routineId", 0)
        val routineName = intent.getStringExtra("routineName") ?: "Rutina"

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = routineName
        toolbar.setNavigationOnClickListener { finish() }

        tvEmpty = findViewById(R.id.tvEmpty)
        tvTitle = findViewById(R.id.tvTitle)
        tvTotal = findViewById(R.id.tvTotal)
        rvExercises = findViewById(R.id.rvRoutineExercises)
        chipGroupDays = findViewById(R.id.chipGroupDays)
        scrollDays = findViewById(R.id.scrollDays)
        btnStart = findViewById(R.id.btnStartWorkout)

        adapter = RoutineExerciseAdapter(
            onClick = { exercise ->
                val intent = Intent(this, ExerciseDetailActivity::class.java)
                intent.putExtra("exercise", exercise)
                startActivity(intent)
            },
            onDelete = { routineExercise -> confirmRemoveExercise(routineExercise) },
            onEditSets = { routineExercise -> showEditDialog(routineExercise, "sets") },
            onEditReps = { routineExercise -> showEditDialog(routineExercise, "reps") }
        )
        rvExercises.layoutManager = LinearLayoutManager(this)
        rvExercises.adapter = adapter

        findViewById<MaterialButton>(R.id.btnAddExercise).setOnClickListener {
            startActivity(Intent(this, AddExerciseToRoutineActivity::class.java).putExtra("routineId", routineId))
        }

        btnStart.setOnClickListener {
            // Si el selector de días es visible (es plan semanal) y está en "Todo", pedimos elegir uno
            if (scrollDays.visibility == View.VISIBLE && currentFilter == "Todo") {
                Toast.makeText(this, "Por favor, selecciona un día (Lunes, Mié o Vie) para iniciar", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, WorkoutSessionActivity::class.java)
                intent.putExtra("routineId", routineId)
                intent.putExtra("selectedDay", currentFilter)
                startActivity(intent)
            }
        }

        chipGroupDays.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = when (checkedId) {
                R.id.chipMon -> "Lunes"
                R.id.chipWed -> "Miércoles"
                R.id.chipFri -> "Viernes"
                else -> "Todo"
            }
            applyFilter()
        }
    }

    override fun onResume() {
        super.onResume()
        loadRoutine()
    }

    private fun loadRoutine() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val routine = db.routineDao().getRoutineById(routineId) ?: return@launch

            tvTitle.text = routine.routine.name
            
            // Si tiene ejercicios, mostramos el botón de iniciar
            btnStart.visibility = if (routine.exercises.isNotEmpty()) View.VISIBLE else View.GONE

            // Si es generada y tiene etiquetas de día, mostramos el filtro
            val hasDays = routine.exercises.any { it.dayLabel.isNotEmpty() && it.dayLabel != "Todo" }
            scrollDays.visibility = if (hasDays) View.VISIBLE else View.GONE

            val exercises = routine.exercises.sortedBy { it.orderIndex }
            allExercisesList = exercises.map { re ->
                val exercise = ExerciseData.getById(re.exerciseId)
                RoutineExerciseWithDetails(re, exercise)
            }
            
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = if (currentFilter == "Todo") {
            allExercisesList
        } else {
            allExercisesList.filter { it.entity.dayLabel == currentFilter }
        }

        adapter.submitList(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvExercises.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

        val totalSets = filtered.sumOf { it.entity.sets }
        tvTotal.text = "${filtered.size} ejercicios | $totalSets series"
    }

    private fun confirmRemoveExercise(item: RoutineExerciseWithDetails) {
        AlertDialog.Builder(this, R.style.Theme_FITCORE)
            .setTitle("Eliminar ejercicio")
            .setMessage("¿Eliminar '${item.exercise?.name ?: "ejercicio"}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(applicationContext)
                    db.routineDao().deleteRoutineExercises(routineId)
                    val updated = allExercisesList.filter { it.entity.id != item.entity.id }.map { it.entity }
                    db.routineDao().insertRoutineExercises(updated)
                    loadRoutine()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditDialog(item: RoutineExerciseWithDetails, field: String) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(if (field == "sets") item.entity.sets.toString() else item.entity.reps.toString())
        }

        AlertDialog.Builder(this, R.style.Theme_FITCORE)
            .setTitle(if (field == "sets") "Series" else "Repeticiones")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val value = input.text.toString().toIntOrNull() ?: return@setPositiveButton
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val updatedEntity = if (field == "sets") item.entity.copy(sets = value) else item.entity.copy(reps = value)
                    db.routineDao().deleteRoutineExercises(routineId)
                    val updatedList = allExercisesList.map { 
                        if (it.entity.id == item.entity.id) updatedEntity else it.entity 
                    }
                    db.routineDao().insertRoutineExercises(updatedList)
                    loadRoutine()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    data class RoutineExerciseWithDetails(
        val entity: RoutineExerciseEntity,
        val exercise: Exercise?
    )

    inner class RoutineExerciseAdapter(
        private val onClick: (Exercise) -> Unit,
        private val onDelete: (RoutineExerciseWithDetails) -> Unit,
        private val onEditSets: (RoutineExerciseWithDetails) -> Unit,
        private val onEditReps: (RoutineExerciseWithDetails) -> Unit
    ) : ListAdapter<RoutineExerciseWithDetails, RoutineExerciseAdapter.ViewHolder>(DiffCallback()) {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.cardExercise)
            val tvName: TextView = view.findViewById(R.id.tvExerciseName)
            val tvMuscle: TextView = view.findViewById(R.id.tvMuscleGroup)
            val tvSets: TextView = view.findViewById(R.id.tvSets)
            val tvReps: TextView = view.findViewById(R.id.tvReps)
            val btnDelete: View = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_routine_exercise, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            val ex = item.exercise

            holder.tvName.text = ex?.name ?: "Ejercicio"
            holder.tvMuscle.text = if (item.entity.dayLabel.isNotEmpty()) "${ex?.muscleGroup} • ${item.entity.dayLabel}" else ex?.muscleGroup
            if (ex != null) holder.tvMuscle.setTextColor(getMuscleColor(ex.muscleGroup))

            holder.tvSets.text = "${item.entity.sets} ×"
            holder.tvReps.text = "${item.entity.reps} reps"

            holder.card.setOnClickListener { ex?.let { onClick(it) } }
            holder.btnDelete.setOnClickListener { onDelete(item) }
            holder.tvSets.setOnClickListener { onEditSets(item) }
            holder.tvReps.setOnClickListener { onEditReps(item) }
        }

        private fun getMuscleColor(group: String): Int = when (group) {
            "Pecho" -> ContextCompat.getColor(this@RoutineDetailActivity, R.color.tag_chest)
            "Espalda" -> ContextCompat.getColor(this@RoutineDetailActivity, R.color.tag_back)
            "Piernas" -> ContextCompat.getColor(this@RoutineDetailActivity, R.color.tag_legs)
            "Hombros" -> ContextCompat.getColor(this@RoutineDetailActivity, R.color.tag_shoulders)
            "Brazos" -> ContextCompat.getColor(this@RoutineDetailActivity, R.color.tag_arms)
            "Core" -> ContextCompat.getColor(this@RoutineDetailActivity, R.color.tag_core)
            else -> ContextCompat.getColor(this@RoutineDetailActivity, R.color.neon_cyan)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RoutineExerciseWithDetails>() {
        override fun areItemsTheSame(a: RoutineExerciseWithDetails, b: RoutineExerciseWithDetails) = a.entity.id == b.entity.id
        override fun areContentsTheSame(a: RoutineExerciseWithDetails, b: RoutineExerciseWithDetails) = a == b
    }
}
