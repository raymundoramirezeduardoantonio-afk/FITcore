package com.example.fitcore.ui.routines

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitcore.R
import com.example.fitcore.agent.RoutineAgents
import com.example.fitcore.agent.RoutinePlanMode
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.db.RoutineEntity
import com.example.fitcore.db.RoutineExerciseEntity
import com.example.fitcore.db.RoutineWithExercises
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RoutinesFragment : Fragment() {

    private lateinit var adapter: RoutineAdapter
    private lateinit var rvRoutines: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvLabelGenerated: TextView
    private lateinit var containerGenerated: LinearLayout
    private lateinit var tvLabelPersonal: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_routines, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvRoutines = view.findViewById(R.id.rvRoutines)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvLabelGenerated = view.findViewById(R.id.tvLabelGenerated)
        containerGenerated = view.findViewById(R.id.containerGenerated)
        tvLabelPersonal = view.findViewById(R.id.tvLabelPersonal)
        
        val fabCreate = view.findViewById<FloatingActionButton>(R.id.fabCreateRoutine)
        val btnGenerate = view.findViewById<MaterialButton>(R.id.btnGenerateRoutine)

        adapter = RoutineAdapter(
            onClick = { routine -> openDetail(routine) },
            onDelete = { routine -> confirmDelete(routine) }
        )

        rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        rvRoutines.adapter = adapter

        fabCreate.setOnClickListener {
            startActivity(Intent(requireContext(), CreateRoutineActivity::class.java))
        }

        btnGenerate.setOnClickListener {
            showGenerationOptions()
        }
    }

    override fun onResume() {
        super.onResume()
        loadRoutines()
    }

    private fun openDetail(routine: RoutineEntity) {
        val intent = Intent(requireContext(), RoutineDetailActivity::class.java)
        intent.putExtra("routineId", routine.id)
        intent.putExtra("routineName", routine.name)
        startActivity(intent)
    }

    private fun loadRoutines() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.routineDao().getAllRoutines().collectLatest { allRoutines ->
                // Separamos rutinas generadas y personales
                val generatedList = allRoutines.filter { it.routine.isGenerated }.sortedByDescending { it.routine.createdAt }
                val personalList = allRoutines.filter { !it.routine.isGenerated }

                // Mostramos TODAS las generadas en el contenedor superior
                if (generatedList.isNotEmpty()) {
                    tvLabelGenerated.visibility = View.VISIBLE
                    containerGenerated.visibility = View.VISIBLE
                    containerGenerated.removeAllViews()
                    for (item in generatedList) {
                        val cardView = createGeneratedCard(item)
                        containerGenerated.addView(cardView)
                    }
                } else {
                    tvLabelGenerated.visibility = View.GONE
                    containerGenerated.visibility = View.GONE
                }

                // Mostramos las personales en el RecyclerView
                adapter.submitList(personalList)
                tvEmpty.visibility = if (personalList.isEmpty()) View.VISIBLE else View.GONE
                rvRoutines.visibility = if (personalList.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun createGeneratedCard(item: RoutineWithExercises): View {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_routine, containerGenerated, false)
        val card: MaterialCardView = view.findViewById(R.id.cardRoutine)
        val tvName: TextView = view.findViewById(R.id.tvRoutineName)
        val tvCount: TextView = view.findViewById(R.id.tvExerciseCount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val btnDelete: View = view.findViewById(R.id.btnDelete)

        tvName.text = item.routine.name
        tvCount.text = "${item.exercises.size} ejercicios"
        
        val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(item.routine.createdAt))
        tvDate.text = "Plan Inteligente ($dateStr)"
        
        card.strokeColor = resources.getColor(R.color.neon_cyan, null)
        card.strokeWidth = 4
        
        card.setOnClickListener { openDetail(item.routine) }
        btnDelete.setOnClickListener { confirmDelete(item.routine) }
        
        return view
    }

    private fun showGenerationOptions() {
        val dialog = BottomSheetDialog(requireContext(), R.style.Theme_FITCORE)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_generation, null)
        
        view.findViewById<View>(R.id.btnOptionFullBody).setOnClickListener {
            generateRoutine(isWeekly = false)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btnOptionWeekly).setOnClickListener {
            generateRoutine(isWeekly = true)
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun generateRoutine(isWeekly: Boolean) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val user = db.routineDao().getLoggedInUser()
            
            if (user == null || user.bodyType.isEmpty()) {
                Toast.makeText(context, "Configura tu biotipo en Perfil primero", Toast.LENGTH_LONG).show()
                return@launch
            }

            val mode = if (isWeekly) RoutinePlanMode.Weekly else RoutinePlanMode.FullBody
            val plan = RoutineAgents.create().plan(user, mode)
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            val routineId = db.routineDao().insertRoutine(
                RoutineEntity(
                    userId = user.id,
                    name = "${plan.name} ($time)",
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
            Toast.makeText(context, "¡Rutina Inteligente Generada!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(routine: RoutineEntity) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_FITCORE)
            .setTitle("Eliminar rutina")
            .setMessage("¿Eliminar '${routine.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    AppDatabase.getDatabase(requireContext()).routineDao().deleteRoutine(routine)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    inner class RoutineAdapter(
        private val onClick: (RoutineEntity) -> Unit,
        private val onDelete: (RoutineEntity) -> Unit
    ) : ListAdapter<RoutineWithExercises, RoutineAdapter.ViewHolder>(RoutineDiffCallback()) {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.cardRoutine)
            val tvName: TextView = view.findViewById(R.id.tvRoutineName)
            val tvCount: TextView = view.findViewById(R.id.tvExerciseCount)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val btnDelete: View = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_routine, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.tvName.text = item.routine.name
            holder.tvCount.text = "${item.exercises.size} ejercicios"
            val date = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
                .format(java.util.Date(item.routine.createdAt))
            holder.tvDate.text = date

            holder.card.setOnClickListener { onClick(item.routine) }
            holder.btnDelete.setOnClickListener { onDelete(item.routine) }
        }
    }

    class RoutineDiffCallback : DiffUtil.ItemCallback<RoutineWithExercises>() {
        override fun areItemsTheSame(a: RoutineWithExercises, b: RoutineWithExercises) = a.routine.id == b.routine.id
        override fun areContentsTheSame(a: RoutineWithExercises, b: RoutineWithExercises) = a == b
    }
}
