package com.example.fitcore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ExerciseAdapter(
    private val onClick: (Exercise) -> Unit
) : ListAdapter<Exercise, ExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvExerciseName)
        private val tvMuscleGroup: TextView = itemView.findViewById(R.id.tvMuscleGroup)
        private val tvEquipment: TextView = itemView.findViewById(R.id.tvEquipment)
        private val tvDifficulty: TextView = itemView.findViewById(R.id.tvDifficulty)
        private val viewAccent: View = itemView.findViewById(R.id.viewAccent)

        fun bind(exercise: Exercise) {
            tvName.text = exercise.name
            tvMuscleGroup.text = exercise.muscleGroup
            tvEquipment.text = exercise.equipment
            tvDifficulty.text = exercise.difficulty

            val ctx = itemView.context
            val color = when (exercise.muscleGroup) {
                "Pecho" -> ContextCompat.getColor(ctx, R.color.tag_chest)
                "Espalda" -> ContextCompat.getColor(ctx, R.color.tag_back)
                "Piernas" -> ContextCompat.getColor(ctx, R.color.tag_legs)
                "Hombros" -> ContextCompat.getColor(ctx, R.color.tag_shoulders)
                "Brazos" -> ContextCompat.getColor(ctx, R.color.tag_arms)
                "Core" -> ContextCompat.getColor(ctx, R.color.tag_core)
                else -> ContextCompat.getColor(ctx, R.color.neon_cyan)
            }
            viewAccent.setBackgroundColor(color)
            tvMuscleGroup.setTextColor(color)

            val (diffColor, bgRes) = when (exercise.difficulty) {
                "Principiante" -> Pair(
                    ContextCompat.getColor(ctx, R.color.difficulty_beginner),
                    R.drawable.bg_badge_beginner
                )
                "Intermedio" -> Pair(
                    ContextCompat.getColor(ctx, R.color.difficulty_intermediate),
                    R.drawable.bg_badge_intermediate
                )
                "Avanzado" -> Pair(
                    ContextCompat.getColor(ctx, R.color.difficulty_advanced),
                    R.drawable.bg_badge_advanced
                )
                else -> Pair(
                    ContextCompat.getColor(ctx, R.color.text_secondary),
                    R.drawable.bg_badge_beginner
                )
            }
            tvDifficulty.setTextColor(diffColor)
            tvDifficulty.setBackgroundResource(bgRes)

            itemView.setOnClickListener { onClick(exercise) }
        }
    }

    class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise) = oldItem == newItem
    }
}
