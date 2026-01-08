package com.example.weightbalance2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.weightbalance2.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var currentDestinationId: Int? = null

    // Konstanten für SharedPreferences
    companion object {
        const val PREFS_NAME = "AppPrefs"
        const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    }

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
        checkAndShowDisclaimer()
    }

    private fun checkAndShowDisclaimer() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasAccepted = prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)

        // Nur beim ersten Start anzeigen (wenn noch nicht akzeptiert)
        if (!hasAccepted) {
            showDisclaimerDialog()
        }
    }

    private fun showDisclaimerDialog() {
        // Inflater für das Custom Layout holen
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.dialog_disclaimer, null)
        val checkbox = dialogView.findViewById<CheckBox>(R.id.doNotShowAgainCheckbox)

        // MaterialAlertDialogBuilder für den modernen Look verwenden
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.disclaimer_title)
            .setView(dialogView) // Custom View setzen
            .setCancelable(false) // Nutzer zwingen, eine Entscheidung zu treffen
            .setPositiveButton(R.string.disclaimer_accept_button) { dialog, _ ->
                // Wenn der Haken gesetzt ist, die Entscheidung in SharedPreferences speichern
                if (checkbox.isChecked) {
                    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, true).apply()
                }
                dialog.dismiss()
            }
            .show()
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
        menu.findItem(R.id.show_disclaimer)?.isVisible = showMenu

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.show_disclaimer) {
            showDisclaimerDialog()
            return true
        }

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
