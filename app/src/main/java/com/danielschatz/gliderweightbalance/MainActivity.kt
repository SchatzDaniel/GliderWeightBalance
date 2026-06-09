package com.danielschatz.gliderweightbalance

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.danielschatz.gliderweightbalance.databinding.ActivityMainBinding
import com.danielschatz.gliderweightbalance.updater.UpdateManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var currentDestinationId: Int? = null
    private val sharedViewModel: SharedViewModel by viewModels()

    // Konstanten für SharedPreferences
    companion object {
        const val PREFS_NAME = "AppPrefs"
        const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Manuelle Inset-Handhabung für die AppBar, um "Jumps" beim Fragment-Wechsel zu vermeiden
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }

        // Toolbar als ActionBar setzen
        setSupportActionBar(binding.toolbar)

        updateHeaderColors()

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

        // Updater initialisieren
        UpdateManager.scheduleUpdateCheck(this)
        requestNotificationPermission()

        // Listener für Fragment-Wechsel
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentDestinationId = destination.id
            invalidateOptionsMenu() // Menü neu vorbereiten
            when (destination.id) {
                R.id.homeFragment -> { // Prüfe die ID in deinem NavGraph!
                    updateAppBarTitleWithSelectedProfile()
                }
            }
        }
        checkAndShowDisclaimer()

        // In der onCreate der MainActivity
        sharedViewModel.selectedProfile.observe(this) { profile ->
            if(navController.currentDestination?.id == R.id.homeFragment) {
                if (profile != null) {
                    val title =
                        "${profile.aircraft.registration} (${profile.aircraft.aircraftType})"
                    setToolbarTitle(title)
                } else {
                    setToolbarTitle(getString(R.string.no_aircraft_selected_title))
                }
            }
        }

    }

    private fun checkAndShowDisclaimer() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
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
                    val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    prefs.edit { putBoolean(KEY_DISCLAIMER_ACCEPTED, true) }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun updateAppBarTitleWithSelectedProfile() {
        val profile = sharedViewModel.selectedProfile.value
        val title = if (profile != null) {
            "${profile.aircraft.registration} (${profile.aircraft.aircraftType})"
        } else {
            getString(R.string.no_aircraft_selected_title)
        }
        binding.toolbar.title = title
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
        menu.findItem(R.id.show_about)?.isVisible = showMenu

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_disclaimer -> {
                showDisclaimerDialog()
                return true
            }
            R.id.show_about -> {
                showAboutDialog()
                return true
            }
        }

        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item)
    }

    private fun showAboutDialog() {
        val versionName = try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "N/A"
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val latestVersion = prefs.getString("latest_available_version", null)
        val updateInfo = if (latestVersion != null) {
            getString(R.string.about_update_available, latestVersion)
        } else ""

        val aboutMessage = TextUtils.concat(
            getString(R.string.about_version, versionName),
            "\n\n",
            getText(R.string.about_info),
            updateInfo
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_title)
            .setMessage(aboutMessage)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.update_button) { _, _ ->
                UpdateManager.runUpdateCheckNow(this)
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * Ermöglicht es den Fragmenten (und der MainActivity), den Titel konsistent zu setzen.
     */
    private fun setToolbarTitle(title: String) {
        binding.toolbar.title = title
        supportActionBar?.title = title
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateHeaderColors() {
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary)

        // StatusBar auf transparent setzen, damit die AppBar (mit fitsSystemWindows) dahinter zeichnet
        window.statusBarColor = Color.TRANSPARENT

        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        // WICHTIG: Da wir colorPrimary als Hintergrund nutzen, müssen wir die Icon-Helligkeit anpassen.
        // In deinem Theme ist Primary im Light-Mode DUNKEL (weiße Icons nötig) 
        // und im Dark-Mode HELL (dunkle Icons nötig).
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isDarkMode

        // AppBar Hintergrund fest auf primaryColor setzen, um Farbwechsel beim Scrollen zu verhindern
        binding.appBarLayout.setBackgroundColor(primaryColor)
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}
