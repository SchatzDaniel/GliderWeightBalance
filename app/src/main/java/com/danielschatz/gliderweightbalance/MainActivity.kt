package com.danielschatz.gliderweightbalance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.danielschatz.gliderweightbalance.databinding.ActivityMainBinding
import com.danielschatz.gliderweightbalance.updater.UpdateManager
import com.danielschatz.gliderweightbalance.utils.PdfExporter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.io.File
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var currentDestinationId: Int? = null
    private val sharedViewModel: SharedViewModel by viewModels()
    private var viewPager: ViewPager2? = null

    private val navBar: NavigationBarView
        get() = (findViewById<NavigationBarView>(R.id.bottomNavigation) ?: findViewById<NavigationBarView>(R.id.navigationRail))!!

    fun setupViewPagerWithBottomNav(pager: ViewPager2?) {
        this.viewPager = pager
        
        pager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val menuId = when (position) {
                    0 -> R.id.aircraftFragment
                    1 -> R.id.homeFragment
                    else -> null
                }
                menuId?.let { 
                    if (navBar.selectedItemId != it) {
                        navBar.selectedItemId = it
                    }
                }
                updateAppBarTitleWithSelectedProfile()
            }
        })
    }

    companion object {
        const val PREFS_NAME = "AppPrefs"
        const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("theme", "system")
        val themeMode = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }

        setSupportActionBar(binding.toolbar)
        updateHeaderColors()

        // Insets für NavigationRail (Querformat)
        if (navBar is NavigationRailView) {
            ViewCompat.setOnApplyWindowInsetsListener(navBar) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(top = insets.top, bottom = insets.bottom)
                windowInsets
            }
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // MainPagerFragment als Top-Level definieren
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.mainPagerFragment))

        NavigationUI.setupWithNavController(
            binding.toolbar,
            navController,
            appBarConfiguration
        )

        NavigationUI.setupWithNavController(
            navBar,
            navController
        )

        navBar.setOnItemSelectedListener { item ->
            val destinationId = item.itemId

            if (destinationId == R.id.scenarioFragment) {
                ScenarioBottomSheetFragment().show(supportFragmentManager, "ScenarioBottomSheet")
                return@setOnItemSelectedListener false
            }

            // Wenn wir uns in einem Untermenü befinden (z.B. AddAircraft),
            // kehren wir erst zum Pager zurück, bevor wir den Tab wechseln.
            if (currentDestinationId != R.id.mainPagerFragment) {
                navController.popBackStack(R.id.mainPagerFragment, false)
            }

            // Jetzt sicher den Tab wechseln
            when (destinationId) {
                R.id.aircraftFragment -> viewPager?.currentItem = 0
                R.id.homeFragment -> viewPager?.currentItem = 1
            }
            true
        }

        UpdateManager.scheduleUpdateCheck(this)
        requestNotificationPermission()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentDestinationId = destination.id
            invalidateOptionsMenu()

            val isPagerActive = destination.id == R.id.mainPagerFragment
            navBar.visibility = if (isPagerActive) android.view.View.VISIBLE else android.view.View.GONE

            if (isPagerActive) {
                // Nur synchronisieren, wenn der ViewPager aktuell registriert ist.
                // Ansonsten lassen wir die BottomNav ihren wiederhergestellten Zustand behalten.
                viewPager?.let { pager ->
                    val currentTab = pager.currentItem
                    val expectedId = if (currentTab == 0) R.id.aircraftFragment else R.id.homeFragment
                    if (navBar.selectedItemId != expectedId) {
                        navBar.selectedItemId = expectedId
                    }
                }
            }

            updateAppBarTitleWithSelectedProfile()
        }

        // Handle Window Insets properly
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            
            binding.appBarLayout.updatePadding(top = systemBars.top)
            
            // Das gesamte Activity-Layout nach oben schieben, wenn die Tastatur erscheint
            view.updatePadding(bottom = imeInsets.bottom)
            
            val isPagerActive = currentDestinationId == R.id.mainPagerFragment
            if (isKeyboardVisible && navBar is BottomNavigationView) {
                navBar.visibility = android.view.View.GONE
            } else if (isPagerActive) {
                navBar.visibility = android.view.View.VISIBLE
            }
            
            if (isPagerActive) {
                viewPager?.let { pager ->
                    val currentTab = pager.currentItem
                    val expectedId = if (currentTab == 0) R.id.aircraftFragment else R.id.homeFragment
                    if (navBar.selectedItemId != expectedId) {
                        navBar.selectedItemId = expectedId
                    }
                }
            }

            insets
        }

        sharedViewModel.selectedProfile.observe(this) { 
            updateAppBarTitleWithSelectedProfile()
        }

        checkAndShowDisclaimer()
        handleUpdateIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUpdateIntent(intent)
    }

    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("SHOW_UPDATE_DIALOG", false) == true) {
            UpdateManager.runUpdateCheckNow(this, this) { version, changelog, apkUrl ->
                showUpdateChangelogDialog(version, changelog, apkUrl)
            }
        }
    }

    private fun showUpdateChangelogDialog(version: String, changelog: String, apkUrl: String) {
        val fileName = "GWB_Update_${version}.apk"
        val existingFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.update_changelog_title, version))
            .setMessage(changelog)
            .setCancelable(false)
            .setPositiveButton(R.string.update_button) { _, _ ->
                if (existingFile.exists()) {
                    UpdateManager.checkPermissionAndInstall(this, existingFile) {
                        showPermissionExplanationDialog(manualUrl = apkUrl)
                    }
                } else {
                    UpdateManager.downloadAndInstallApk(this, apkUrl, fileName) {
                        showPermissionExplanationDialog(manualUrl = apkUrl)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPermissionExplanationDialog(manualUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_permission_title)
            .setMessage(R.string.update_permission_msg)
            .setCancelable(false)
            .setPositiveButton(R.string.update_permission_grant) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = "package:$packageName".toUri()
                    }
                    startActivity(intent)
                }
            }
            .setNegativeButton(R.string.update_manual_download) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, manualUrl.toUri())
                startActivity(intent)
            }
            .show()
    }

    private fun checkAndShowDisclaimer() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)) {
            showDisclaimerDialog()
        }
    }

    private fun showDisclaimerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_disclaimer, null)
        val checkbox = dialogView.findViewById<CheckBox>(R.id.doNotShowAgainCheckbox)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.disclaimer_title)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(R.string.disclaimer_accept_button) { dialog, _ ->
                if (checkbox.isChecked) {
                    val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    prefs.edit { putBoolean(KEY_DISCLAIMER_ACCEPTED, true) }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun updateAppBarTitleWithSelectedProfile() {
        if (currentDestinationId != R.id.mainPagerFragment) return

        val profile = sharedViewModel.selectedProfile.value
        val title = when (viewPager?.currentItem) {
            0 -> getString(R.string.title_activity_aircraft)
            1 -> {
                if (profile != null) {
                    "${profile.aircraft.registration} (${profile.aircraft.aircraftType})"
                } else {
                    getString(R.string.no_aircraft_selected_title)
                }
            }
            else -> getString(R.string.app_name)
        }
        binding.toolbar.title = title
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val showMenu = currentDestinationId == R.id.mainPagerFragment
        menu.findItem(R.id.aircraftFragment)?.isVisible = false
        menu.findItem(R.id.settingsFragment)?.isVisible = showMenu
        menu.findItem(R.id.show_disclaimer)?.isVisible = showMenu
        menu.findItem(R.id.show_about)?.isVisible = showMenu
        menu.findItem(R.id.action_export_pdf)?.isVisible = showMenu && sharedViewModel.selectedProfile.value != null
        return super.onPrepareOptionsMenu(menu)
    }

    private fun exportToPdf() {
        val profile = sharedViewModel.selectedProfile.value ?: return
        val totalMassResult = sharedViewModel.totalMass.value
        val cgLocationResult = sharedViewModel.cg.value
        val nonLiftingMassResult = sharedViewModel.nonLiftingMass.value
        
        if (totalMassResult !is CalculationResult.Success || cgLocationResult !is CalculationResult.Success) return

        val totalMass = totalMassResult.value
        val totalMoment = totalMass * cgLocationResult.value
        val nonLiftingMass = if (nonLiftingMassResult is CalculationResult.Success) nonLiftingMassResult.value else 0.0

        val aircraft = profile.aircraft
        val minLimitCg = aircraft.minCg ?: 0.0
        val maxLimitCg = aircraft.maxCg ?: 0.0
        val hasCgLimit = aircraft.minCg != null || aircraft.maxCg != null

        val isCgOk = !hasCgLimit || (cgLocationResult.value in minLimitCg..maxLimitCg)
        val isTotalMassOk = aircraft.maxTotalMass == null || totalMass <= aircraft.maxTotalMass
        val isNonLiftingOk = aircraft.maxNonLiftingMass == null || nonLiftingMass <= aircraft.maxNonLiftingMass
        
        var isWithinLimits = isCgOk && isTotalMassOk && isNonLiftingOk

        // WICHTIG: Falls es eine Range im Flug gibt, muss auch diese komplett innerhalb der Limits liegen
        val cgRange = sharedViewModel.cgRange.value
        if (cgRange != null && hasCgLimit) {
            if (cgRange.first < minLimitCg || cgRange.second > maxLimitCg) {
                isWithinLimits = false
            }
        }

        // CG in % MAC berechnen
        val range = maxLimitCg - minLimitCg
        val cgMac = if (range > 0 && hasCgLimit) (cgLocationResult.value - minLimitCg) / range * 100 else null

        val exporter = PdfExporter(this)
        val pdfFile = exporter.generateReport(
            profile,
            totalMass,
            totalMoment,
            nonLiftingMass,
            cgLocationResult.value,
            cgMac,
            cgRange,
            isWithinLimits
        )

        if (pdfFile != null) {
            val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.menu_item_export_pdf)))
        } else {
            Toast.makeText(this, R.string.pdf_export_error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_disclaimer -> { showDisclaimerDialog(); return true }
            R.id.show_about -> { showAboutDialog(); return true }
            R.id.action_export_pdf -> { exportToPdf(); return true }
        }
        return NavigationUI.onNavDestinationSelected(item, navController) || super.onOptionsItemSelected(item)
    }

    private fun showAboutDialog() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) { "N/A" }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val latestVersion = prefs.getString("latest_available_version", null)
        val updateInfo = if (latestVersion != null) getString(R.string.about_update_available, latestVersion) else ""

        val aboutMessage = TextUtils.concat(getString(R.string.about_version, versionName), "\n\n", getText(R.string.about_info), updateInfo)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_title)
            .setMessage(aboutMessage)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.update_button) { _, _ ->
                UpdateManager.runUpdateCheckNow(this, this) { version, changelog, apkUrl ->
                    showUpdateChangelogDialog(version, changelog, apkUrl)
                }
            }
            .show()
    }

    override fun onSupportNavigateUp() = navController.navigateUp() || super.onSupportNavigateUp()

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateHeaderColors() {
        val primaryColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary)
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        // Da enableEdgeToEdge() verwendet wird, ist die StatusBar bereits transparent.
        // Wir steuern nur noch die Helligkeit der Icons (hell/dunkel).
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
        binding.appBarLayout.setBackgroundColor(primaryColor)
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}
