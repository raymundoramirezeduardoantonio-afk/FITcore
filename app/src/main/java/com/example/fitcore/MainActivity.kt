package com.example.fitcore

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.fitcore.db.AppDatabase
import com.example.fitcore.ui.catalog.CatalogFragment
import com.example.fitcore.ui.home.HomeFragment
import com.example.fitcore.ui.profile.ProfileFragment
import com.example.fitcore.ui.routines.RoutinesFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var homeFragment: HomeFragment
    private lateinit var catalogFragment: CatalogFragment
    private lateinit var routinesFragment: RoutinesFragment
    private lateinit var profileFragment: ProfileFragment
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        checkLogin(savedInstanceState)
    }

    private fun checkLogin(savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val user = db.routineDao().getLoggedInUser()
            if (user == null) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            } else {
                initUI(savedInstanceState)
            }
        }
    }

    private fun initUI(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragmentContainer)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top)
            insets
        }

        // La barra flotante se separa de la barra de navegación del sistema
        val navContainer = findViewById<View>(R.id.navContainer)
        val baseMargin = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(navContainer) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = baseMargin + bars.bottom }
            insets
        }
        navContainer.translationY = 300f
        navContainer.animate().translationY(0f).setStartDelay(250).setDuration(600)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.1f)).start()

        if (savedInstanceState == null) {
            // Primera vez, creamos instancias
            homeFragment = HomeFragment()
            catalogFragment = CatalogFragment()
            routinesFragment = RoutinesFragment()
            profileFragment = ProfileFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
                .add(R.id.fragmentContainer, routinesFragment, "routines").hide(routinesFragment)
                .add(R.id.fragmentContainer, catalogFragment, "catalog").hide(catalogFragment)
                .add(R.id.fragmentContainer, homeFragment, "home")
                .commit()
            
            activeFragment = homeFragment
        } else {
            // Restauración automática del sistema
            homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment ?: HomeFragment()
            catalogFragment = supportFragmentManager.findFragmentByTag("catalog") as? CatalogFragment ?: CatalogFragment()
            routinesFragment = supportFragmentManager.findFragmentByTag("routines") as? RoutinesFragment ?: RoutinesFragment()
            profileFragment = supportFragmentManager.findFragmentByTag("profile") as? ProfileFragment ?: ProfileFragment()
            
            // Intentar recuperar el fragmento activo o por defecto Home
            activeFragment = supportFragmentManager.fragments.firstOrNull { !it.isHidden } ?: homeFragment
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.nav_catalog -> {
                    switchFragment(catalogFragment)
                    true
                }
                R.id.nav_routines -> {
                    switchFragment(routinesFragment)
                    true
                }
                R.id.nav_profile -> {
                    switchFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(target: Fragment) {
        if (target == activeFragment) return
        val transaction = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
        
        activeFragment?.let { transaction.hide(it) }
        transaction.show(target).commit()

        activeFragment = target
    }

    /** Cambia de pestaña desde cualquier fragmento. */
    fun navigateTo(itemId: Int) {
        findViewById<BottomNavigationView>(R.id.bottomNav)?.selectedItemId = itemId
    }

    /** Abre el catálogo filtrado por un grupo muscular. */
    fun openCatalog(group: String) {
        navigateTo(R.id.nav_catalog)
        catalogFragment.selectGroup(group)
    }
}
