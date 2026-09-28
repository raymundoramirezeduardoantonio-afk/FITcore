package com.example.fitcore.ui.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.fitcore.ExerciseData
import com.example.fitcore.MainActivity
import com.example.fitcore.R
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.db.UserEntity
import com.example.fitcore.ui.Motion
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {

    private lateinit var tvWelcomeName: TextView
    private lateinit var tvMotto: TextView
    private lateinit var tvAvatar: TextView
    private lateinit var tvStreakHome: TextView
    private lateinit var tvXPHome: TextView
    private lateinit var tvWorkoutsHome: TextView
    private lateinit var tvLevelHome: TextView
    private lateinit var tvLevelPercent: TextView
    private lateinit var tvXPNextLevel: TextView
    private lateinit var cpLevel: CircularProgressIndicator
    private lateinit var weekStrip: LinearLayout
    private lateinit var muscleRow: LinearLayout
    private lateinit var btnQuickStart: View

    private var lastUser: UserEntity? = null

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
        tvAvatar = view.findViewById(R.id.tvAvatar)
        tvStreakHome = view.findViewById(R.id.tvStreakHome)
        tvXPHome = view.findViewById(R.id.tvXPHome)
        tvWorkoutsHome = view.findViewById(R.id.tvWorkoutsHome)
        tvLevelHome = view.findViewById(R.id.tvLevelHome)
        tvLevelPercent = view.findViewById(R.id.tvLevelPercent)
        tvXPNextLevel = view.findViewById(R.id.tvXPNextLevel)
        cpLevel = view.findViewById(R.id.cpLevel)
        weekStrip = view.findViewById(R.id.weekStrip)
        muscleRow = view.findViewById(R.id.muscleRow)
        btnQuickStart = view.findViewById(R.id.btnQuickStart)

        btnQuickStart.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_routines)
        }
        tvAvatar.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_profile)
        }
        view.findViewById<View>(R.id.tvSeeAll).setOnClickListener {
            (activity as? MainActivity)?.openCatalog("Todos")
        }

        buildMuscleTiles()
        buildWeekStrip(null)
        playEntrance()
        Motion.breathe(view.findViewById(R.id.quickStartArrow), amount = 0.08f, duration = 900L)
        observeUserData()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            playEntrance()
            lastUser?.let { bindStats(it) }
        }
    }

    private fun playEntrance() {
        val root = view ?: return
        Motion.staggerIn(
            listOf(
                root.findViewById(R.id.homeHeader),
                root.findViewById(R.id.heroCard),
                root.findViewById(R.id.statsRow),
                root.findViewById(R.id.lblWeek),
                weekStrip,
                btnQuickStart,
                root.findViewById(R.id.muscleHeader),
                root.findViewById(R.id.muscleScroll)
            ),
            startDelay = 60L
        )
    }

    private fun observeUserData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.routineDao().getLoggedInUserFlow().collectLatest { user ->
                user?.let {
                    lastUser = it
                    val firstName = it.name.trim().substringBefore(' ').ifEmpty { "atleta" }
                    tvWelcomeName.text = "$firstName 👋"
                    tvAvatar.text = firstName.first().uppercaseChar().toString()

                    val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    tvMotto.text = when (hora) {
                        in 5..11 -> "Buenos días"
                        in 12..18 -> "Buenas tardes"
                        else -> "Buenas noches"
                    }

                    buildWeekStrip(it)
                    bindStats(it)
                }
            }
        }
    }

    private fun bindStats(user: UserEntity) {
        val levelFromXp = (user.xp / 200) + 1
        val level = if (user.level == levelFromXp) user.level else levelFromXp
        val progress = (user.xp % 200) * 100 / 200
        val xpRestante = 200 - (user.xp % 200)

        tvXPNextLevel.text = "Faltan $xpRestante XP para el nivel ${level + 1}"
        Motion.countUp(tvLevelHome, level, duration = 700L)
        Motion.countUp(tvLevelPercent, progress) { "$it%" }
        Motion.countUp(tvStreakHome, user.streakDays)
        Motion.countUp(tvXPHome, user.xp)
        Motion.countUp(tvWorkoutsHome, user.workoutsCompleted)

        cpLevel.setProgressCompat(0, false)
        cpLevel.postDelayed({ cpLevel.setProgressCompat(progress, true) }, 250L)
    }

    private fun buildMuscleTiles() {
        val inflater = LayoutInflater.from(requireContext())
        muscleRow.removeAllViews()
        ExerciseData.getMuscleGroups().filter { it != "Todos" }.forEach { group ->
            val tile = inflater.inflate(R.layout.item_muscle_tile, muscleRow, false)
            val color = Motion.muscleColor(tile, group)
            val tint = ColorStateList.valueOf(color)
            tile.findViewById<View>(R.id.tileGlow).backgroundTintList = tint
            tile.findViewById<View>(R.id.tileDot).backgroundTintList = tint
            tile.findViewById<TextView>(R.id.tileName).text = group
            val count = ExerciseData.getByMuscleGroup(group).size
            tile.findViewById<TextView>(R.id.tileCount).text = "$count ejercicios"
            tile.setOnClickListener { (activity as? MainActivity)?.openCatalog(group) }
            muscleRow.addView(tile)
        }
    }

    /** Semana actual (lunes a domingo): hoy resaltado y puntos en los días de la racha. */
    private fun buildWeekStrip(user: UserEntity?) {
        val ctx = requireContext()
        val inflater = LayoutInflater.from(ctx)
        weekStrip.removeAllViews()

        val today = Calendar.getInstance()
        val monday = (today.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (after(today)) add(Calendar.DAY_OF_YEAR, -7)
        }
        val names = listOf("L", "M", "X", "J", "V", "S", "D")
        val trainedDays = trainedDayKeys(user)

        for (i in 0 until 7) {
            val day = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            val item = inflater.inflate(R.layout.item_week_day, weekStrip, false)
            val tvName = item.findViewById<TextView>(R.id.dayName)
            val tvNumber = item.findViewById<TextView>(R.id.dayNumber)
            val dot = item.findViewById<View>(R.id.dayDot)

            tvName.text = names[i]
            tvNumber.text = day.get(Calendar.DAY_OF_MONTH).toString()

            val isToday = sameDay(day, today)
            if (isToday) {
                tvNumber.setBackgroundResource(R.drawable.bg_circle_volt)
                tvNumber.setTextColor(ContextCompat.getColor(ctx, R.color.black))
                tvName.setTextColor(ContextCompat.getColor(ctx, R.color.volt))
            } else if (day.after(today)) {
                tvNumber.setTextColor(ContextCompat.getColor(ctx, R.color.text_tertiary))
            }
            if (dayKey(day) in trainedDays) {
                dot.visibility = View.VISIBLE
                if (isToday) dot.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.flame))
            }
            weekStrip.addView(item)
        }
    }

    private fun trainedDayKeys(user: UserEntity?): Set<Int> {
        if (user == null || user.lastWorkoutTimestamp <= 0L || user.streakDays <= 0) return emptySet()
        val last = Calendar.getInstance().apply { timeInMillis = user.lastWorkoutTimestamp }
        return (0 until user.streakDays.coerceAtMost(7)).map { back ->
            dayKey((last.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -back) })
        }.toSet()
    }

    private fun dayKey(c: Calendar) = c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)

    private fun sameDay(a: Calendar, b: Calendar) = dayKey(a) == dayKey(b)
}
