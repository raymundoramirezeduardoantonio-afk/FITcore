package com.example.fitcore

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.db.UserEntity
import com.example.fitcore.ui.Motion
import com.example.fitcore.util.PasswordUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_register)

        val etName = findViewById<TextInputEditText>(R.id.etRegName)
        val etEmail = findViewById<TextInputEditText>(R.id.etRegEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etRegPassword)
        val etHeight = findViewById<TextInputEditText>(R.id.etRegHeight)
        val etWeight = findViewById<TextInputEditText>(R.id.etRegWeight)
        val toggleBodyType = findViewById<MaterialButtonToggleGroup>(R.id.toggleRegBodyType)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val tvBodyTypeHint = findViewById<TextView>(R.id.tvBodyTypeHint)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        playEntrance()

        toggleBodyType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            tvBodyTypeHint.text = when (checkedId) {
                R.id.btnRegEcto -> "Ectomorfo · complexión delgada, metabolismo rápido. Priorizamos fuerza y volumen moderado."
                R.id.btnRegMeso -> "Mesomorfo · complexión atlética, gana músculo con facilidad. Equilibrio entre fuerza e hipertrofia."
                else -> "Endomorfo · complexión robusta, tiende a acumular grasa. Más densidad y trabajo metabólico."
            }
            Motion.staggerIn(listOf(tvBodyTypeHint), distanceDp = 8f)
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val height = etHeight.text.toString().toFloatOrNull() ?: 0f
            val weight = etWeight.text.toString().toFloatOrNull() ?: 0f
            
            val bodyType = when (toggleBodyType.checkedButtonId) {
                R.id.btnRegEcto -> "Ectomorfo"
                R.id.btnRegMeso -> "Mesomorfo"
                R.id.btnRegEndo -> "Endomorfo"
                else -> ""
            }

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || bodyType.isEmpty()) {
                Motion.shake(btnRegister)
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val existing = db.routineDao().getUserByEmail(email)
                
                if (existing != null) {
                    Toast.makeText(this@RegisterActivity, "Este correo ya está registrado", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = UserEntity(
                        name = name,
                        email = email,
                        password = PasswordUtils.hash(password),
                        height = height,
                        weight = weight,
                        bodyType = bodyType,
                        isLoggedIn = true
                    )
                    db.routineDao().logoutAll()
                    db.routineDao().saveUserProfile(newUser)
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finishAffinity()
                }
            }
        }
    }

    private fun playEntrance() {
        Motion.drift(findViewById(R.id.glowFlame), -70f, 60f, 7500L)
        Motion.drift(findViewById(R.id.glowViolet), 60f, -70f, 8500L)
        Motion.staggerIn(
            listOf(
                findViewById(R.id.btnBack),
                findViewById(R.id.tvRegTitle),
                findViewById(R.id.tvRegSubtitle),
                findViewById(R.id.lblAccount),
                findViewById(R.id.tilName),
                findViewById(R.id.tilRegEmail),
                findViewById(R.id.tilRegPassword),
                findViewById(R.id.lblBody),
                findViewById(R.id.rowMeasures),
                findViewById(R.id.toggleRegBodyType),
                findViewById(R.id.tvBodyTypeHint),
                findViewById(R.id.btnRegister)
            ),
            startDelay = 120L,
            step = 55L
        )
    }
}
