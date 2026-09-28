package com.example.fitcore.ui.catalog

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitcore.ExerciseAdapter
import com.example.fitcore.ExerciseData
import com.example.fitcore.ExerciseDetailActivity
import com.example.fitcore.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class CatalogFragment : Fragment() {

    private lateinit var adapter: ExerciseAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private lateinit var etSearch: TextInputEditText
    private lateinit var tvResultsCount: TextView
    private var selectedGroup = "Todos"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvExercises)
        chipGroup = view.findViewById(R.id.chipGroupFilters)
        etSearch = view.findViewById(R.id.etSearch)
        tvResultsCount = view.findViewById(R.id.tvResultsCount)

        setupChips()
        setupRecyclerView()
        setupSearch()
        applyFilters()
    }

    private fun setupChips() {
        chipGroup.isSelectionRequired = true
        ExerciseData.getMuscleGroups().forEachIndexed { index, group ->
            val chip = Chip(requireContext()).apply {
                text = group
                isCheckable = true
                isChecked = index == 0
                id = View.generateViewId()

                if (index == 0) {
                    setChipBackgroundColorResource(R.color.neon_cyan)
                    setChipStrokeColorResource(R.color.neon_cyan)
                    setTextColor(resources.getColor(R.color.black, null))
                    chipStrokeWidth = 0f
                } else {
                    setChipBackgroundColorResource(R.color.bg_card)
                    setChipStrokeColorResource(R.color.divider)
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    chipStrokeWidth = resources.displayMetrics.density
                }

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedGroup = group
                        applyFilters()
                        updateChipStyles(this)
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun updateChipStyles(selectedChip: Chip) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.id == selectedChip.id) {
                chip.setChipBackgroundColorResource(R.color.neon_cyan)
                chip.setChipStrokeColorResource(R.color.neon_cyan)
                chip.setTextColor(resources.getColor(R.color.black, null))
                chip.chipStrokeWidth = 0f
            } else {
                chip.setChipBackgroundColorResource(R.color.bg_card)
                chip.setChipStrokeColorResource(R.color.divider)
                chip.setTextColor(resources.getColor(R.color.text_primary, null))
                chip.chipStrokeWidth = resources.displayMetrics.density
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ExerciseAdapter { exercise ->
            val intent = Intent(requireContext(), ExerciseDetailActivity::class.java)
            intent.putExtra("exercise", exercise)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null
    }
 
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applyFilters() {
        val query = etSearch.text?.toString() ?: ""
        val filtered = if (selectedGroup == "Todos") {
            ExerciseData.search(query)
        } else {
            ExerciseData.search(query).filter { it.muscleGroup == selectedGroup }
        }
        adapter.submitList(filtered)
        tvResultsCount.text = "${filtered.size} ejercicio${if (filtered.size != 1) "s" else ""}"
    }
}
