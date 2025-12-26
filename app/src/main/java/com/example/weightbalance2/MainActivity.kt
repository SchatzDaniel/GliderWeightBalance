package com.example.weightbalance2

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.weightbalance2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // Speichert das aktuell sichtbare Fragment
    private var currentDestinationId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar als ActionBar setzen
        setSupportActionBar(binding.toolbar)

        // NavController holen
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // HomeFragment ist Root (kein Zurück-Pfeil dort)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment))

        // Toolbar mit Navigation verbinden
        NavigationUI.setupWithNavController(
            binding.toolbar,
            navController,
            appBarConfiguration
        )

        // Listener für Fragment-Wechsel
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentDestinationId = destination.id
            invalidateOptionsMenu() // Menü neu vorbereiten
        }
    }

    override fun onCreateOptionsMenu(menu: Menu):Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val showMenu = currentDestinationId == R.id.homeFragment

        // Menü-Icons nur im HomeFragment anzeigen
        menu.findItem(R.id.aircraftFragment)?.isVisible = showMenu
        menu.findItem(R.id.settingsFragment)?.isVisible = showMenu

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    /**
    * Ermöglicht es den Fragmenten, den Titel der ActionBar zu setzen.
    */
    fun setToolbarTitle(title: String) {
        binding.toolbar.title = title
    }
}
