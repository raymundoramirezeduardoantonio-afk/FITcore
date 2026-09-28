package com.example.fitcore

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.ui.Motion
import com.example.fitcore.util.PasswordUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<TextInputEditText>(R.id.etLoginEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        tvGoToRegister.text = accentText("¿No tienes cuenta? ", "Regístrate")
        playEntrance()

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                shakeForm()
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val user = db.routineDao().getUserByEmail(email)

                if (user != null && PasswordUtils.verify(password, user.password)) {
                    val passwordHash = if (PasswordUtils.isLegacyHash(user.password)) {
                        PasswordUtils.hash(password)
                    } else {
                        user.password
                    }
                    db.routineDao().logoutAll()
                    db.routineDao().saveUserProfile(
                        user.copy(password = passwordHash, isLoggedIn = true)
                    )
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    shakeForm()
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun playEntrance() {
        Motion.drift(findViewById(R.id.glowViolet), -60f, 80f, 7000L)
        Motion.drift(findViewById(R.id.glowVolt), 70f, -50f, 8000L)
        Motion.popIn(findViewById(R.id.logoMark), 100L)
        Motion.staggerIn(
            listOf(
                findViewById(R.id.tvAppName),
                findViewById(R.id.tvAppName2),
                findViewById(R.id.tvTagline),
                findViewById(R.id.tvFormTitle),
                findViewById(R.id.tilEmail),
                findViewById(R.id.tilPassword),
                findViewById(R.id.btnLogin),
                findViewById(R.id.tvGoToRegister)
            ),
            startDelay = 180L
        )
    }

    private fun shakeForm() {
        Motion.shake(findViewById(R.id.tilEmail))
        Motion.shake(findViewById(R.id.tilPassword))
    }

    private fun accentText(plain: String, accent: String): CharSequence {
        val span = SpannableString(plain + accent)
        span.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.volt)),
            plain.length, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        span.setSpan(StyleSpan(Typeface.BOLD), plain.length, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return span
    }
}
