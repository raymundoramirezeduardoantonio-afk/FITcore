package com.example.fitcore.ui.routines

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.fitcore.R
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.db.RoutineEntity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class CreateRoutineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_create_routine)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val etName = findViewById<EditText>(R.id.etRoutineName)
        val etDescription = findViewById<EditText>(R.id.etRoutineDescription)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "Nombre requerido"
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                val user = db.routineDao().getLoggedInUser()
                
                if (user != null) {
                    db.routineDao().insertRoutine(
                        RoutineEntity(
                            userId = user.id, // Vincula al usuario actual
                            name = name,
                            description = etDescription.text.toString().trim()
                        )
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreateRoutineActivity, "Rutina creada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }
}
