package com.example.fitcore.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitcore.R
import com.example.fitcore.db.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {

    private lateinit var tvWelcomeName: TextView
    private lateinit var tvMotto: TextView
    private lateinit var tvStreakHome: TextView
    private lateinit var tvXPHome: TextView
    private lateinit var tvLevelHome: TextView
    private lateinit var tvXPNextLevel: TextView
    private lateinit var cpLevel: CircularProgressIndicator
    private lateinit var btnQuickStart: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvWelcomeName = view.findViewById(R.id.tvWelcomeName)
        tvMotto = view.findViewById(R.id.tvMotto)
        tvStreakHome = view.findViewById(R.id.tvStreakHome)
        tvXPHome = view.findViewById(R.id.tvXPHome)
        tvLevelHome = view.findViewById(R.id.tvLevelHome)
        tvXPNextLevel = view.findViewById(R.id.tvXPNextLevel)
        cpLevel = view.findViewById(R.id.cpLevel)
        btnQuickStart = view.findViewById(R.id.btnQuickStart)

        btnQuickStart.setOnClickListener {
            activity?.findViewById<View>(R.id.nav_routines)?.performClick()
        }

        observeUserData()
    }

    private fun observeUserData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.routineDao().getLoggedInUserFlow().collectLatest { user ->
                user?.let {
                    tvWelcomeName.text = "¡Hola, ${it.name}!"
                    tvStreakHome.text = "${it.streakDays} Días"
                    tvXPHome.text = "${it.xp} XP"
                    
                    val level = (it.xp / 200) + 1
                    val progress = (it.xp % 200) * 100 / 200
                    val xpRestante = 200 - (it.xp % 200)
                    
                    tvLevelHome.text = level.toString()
                    cpLevel.setProgress(progress, true)
                    tvXPNextLevel.text = "Faltan $xpRestante XP para el Nivel ${level + 1}"

                    val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    tvMotto.text = when {
                        hora in 5..11 -> "¡Buenos días! Es momento de energía."
                        hora in 12..18 -> "¡Buenas tardes! Mantén el ritmo alto."
                        else -> "Buenas noches. Cierra con un gran entreno."
                    }
                }
            }
        }
    }
}
