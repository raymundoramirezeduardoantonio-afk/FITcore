package com.example.fitcore.ui

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.view.animation.PathInterpolator
import com.example.fitcore.R

/** Pequeñas animaciones reutilizables para dar vida a la interfaz. */
object Motion {

    private val smooth = PathInterpolator(0.4f, 0f, 0.2f, 1f)

    /** Hace aparecer las vistas desde abajo, una tras otra. */
    fun staggerIn(views: List<View?>, startDelay: Long = 0L, step: Long = 70L, distanceDp: Float = 28f) {
        views.filterNotNull().forEachIndexed { index, view ->
            val distance = distanceDp * view.resources.displayMetrics.density
            view.animate().cancel()
            view.alpha = 0f
            view.translationY = distance
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(startDelay + index * step)
                .setDuration(520L)
                .setInterpolator(smooth)
                .start()
        }
    }

    /** Aparece con un pequeño rebote de escala. */
    fun popIn(view: View, startDelay: Long = 0L) {
        view.animate().cancel()
        view.alpha = 0f
        view.scaleX = 0.6f
        view.scaleY = 0.6f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(startDelay)
            .setDuration(480L)
            .setInterpolator(OvershootInterpolator(2.2f))
            .start()
    }

    /** Cuenta un número de forma animada dentro de un TextView. */
    fun countUp(textView: TextView, to: Int, duration: Long = 1100L, format: (Int) -> String = { it.toString() }) {
        (textView.getTag(R.id.tag_count_animator) as? ValueAnimator)?.cancel()
        if (to <= 0) {
            textView.text = format(to)
            return
        }
        val animator = ValueAnimator.ofInt(0, to).apply {
            this.duration = duration
            interpolator = smooth
            addUpdateListener { textView.text = format(it.animatedValue as Int) }
        }
        textView.setTag(R.id.tag_count_animator, animator)
        animator.start()
    }

    /** Respiración suave e infinita, ideal para el botón principal. */
    fun breathe(view: View, amount: Float = 0.035f, duration: Long = 1400L) {
        (view.getTag(R.id.tag_loop_animator) as? ValueAnimator)?.cancel()
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val s = 1f + amount * (it.animatedValue as Float)
                view.scaleX = s
                view.scaleY = s
            }
        }
        view.setTag(R.id.tag_loop_animator, animator)
        animator.start()
    }

    /** Desplazamiento lento y continuo para los destellos de fondo. */
    fun drift(view: View, dxDp: Float, dyDp: Float, duration: Long = 6000L) {
        val d = view.resources.displayMetrics.density
        (view.getTag(R.id.tag_loop_animator) as? ValueAnimator)?.cancel()
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val f = it.animatedValue as Float
                view.translationX = dxDp * d * f
                view.translationY = dyDp * d * f
                view.rotation = 20f * f
            }
        }
        view.setTag(R.id.tag_loop_animator, animator)
        animator.start()
    }

    /** Sacudida horizontal para indicar un error. */
    fun shake(view: View) {
        val d = view.resources.displayMetrics.density
        ValueAnimator.ofFloat(0f, -10f, 10f, -8f, 8f, -4f, 4f, 0f).apply {
            duration = 420L
            addUpdateListener { view.translationX = (it.animatedValue as Float) * d }
            start()
        }
    }

    /** Detiene las animaciones infinitas asociadas a la vista. */
    fun stopLoop(view: View) {
        (view.getTag(R.id.tag_loop_animator) as? ValueAnimator)?.cancel()
    }

    fun muscleColor(view: View, group: String): Int = ContextCompat.getColor(
        view.context,
        when (group) {
            "Pecho" -> R.color.tag_chest
            "Espalda" -> R.color.tag_back
            "Piernas" -> R.color.tag_legs
            "Hombros" -> R.color.tag_shoulders
            "Brazos" -> R.color.tag_arms
            "Core" -> R.color.tag_core
            else -> R.color.volt
        }
    )
}
