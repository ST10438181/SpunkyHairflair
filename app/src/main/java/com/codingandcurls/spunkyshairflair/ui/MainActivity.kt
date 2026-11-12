package com.codingandcurls.spunkyshairflair.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.codingandcurls.spunkyshairflair.R
import com.codingandcurls.spunkyshairflair.data.Repos
import kotlinx.coroutines.launch

/** Hosts the five bottom-bar tabs: Home, Book, My Bookings, Services, Contact. */
class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView
    private var preselectedServiceId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Repos.users(this).getCurrentUser() == null) {
            startActivity(clearTaskIntent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_book -> BookFragment()
                R.id.nav_bookings -> BookingsFragment()
                R.id.nav_services -> ServicesFragment()
                R.id.nav_contact -> ContactFragment()
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            true
        }
        if (savedInstanceState == null) bottomNav.selectedItemId = R.id.nav_home
    }

    /** Opens the Book tab, optionally with a service already chosen (from Home / Services). */
    fun openBook(serviceId: Int?) {
        preselectedServiceId = serviceId
        bottomNav.selectedItemId = R.id.nav_book
    }

    fun consumePreselectedService(): Int? {
        val id = preselectedServiceId
        preselectedServiceId = null
        return id
    }

    fun showBookings() {
        bottomNav.selectedItemId = R.id.nav_bookings
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        R.id.action_help -> {
            startActivity(Intent(this, HelpActivity::class.java))
            true
        }
        R.id.action_logout -> {
            MaterialAlertDialogBuilder(this)
                .setTitle("Log out?")
                .setMessage("You will need to log in again to book appointments.")
                .setPositiveButton("Log out") { _, _ ->
                    lifecycleScope.launch {
                        Repos.users(this@MainActivity).logout()
                        startActivity(clearTaskIntent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    }
                }
                .setNegativeButton("Stay", null)
                .show()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
