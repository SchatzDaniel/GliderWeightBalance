package com.danielschatz.gliderweightbalance

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var currentDestinationId: Int? = null
    private val sharedViewModel: SharedViewModel by viewModels()

    companion object {
        const val PREFS_NAME = "AppPrefs"
        const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment))

        NavigationUI.setupWithNavController(
            binding.toolbar,
            navController,
            appBarConfiguration
        )

        UpdateManager.scheduleUpdateCheck(this)
        requestNotificationPermission()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentDestinationId = destination.id
            invalidateOptionsMenu()
            if (destination.id == R.id.homeFragment) { 
                updateAppBarTitleWithSelectedProfile()
            }
        }

        sharedViewModel.selectedProfile.observe(this) { 
            if (currentDestinationId == R.id.homeFragment) {
                updateAppBarTitleWithSelectedProfile()
            }
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
                        showPermissionExplanationDialog(apkFile = existingFile, manualUrl = apkUrl)
                    }
                } else {
                    UpdateManager.downloadAndInstallApk(this, apkUrl, fileName) { apkFile ->
                        showPermissionExplanationDialog(apkFile = apkFile, manualUrl = apkUrl)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPermissionExplanationDialog(apkFile: File, manualUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_permission_title)
            .setMessage(R.string.update_permission_msg)
            .setCancelable(false)
            .setPositiveButton(R.string.update_permission_grant) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            }
            .setNegativeButton(R.string.update_manual_download) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(manualUrl))
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
        val profile = sharedViewModel.selectedProfile.value
        val title = if (profile != null) {
            "${profile.aircraft.registration} (${profile.aircraft.aircraftType})"
        } else {
            getString(R.string.no_aircraft_selected_title)
        }
        binding.toolbar.title = title
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val showMenu = currentDestinationId == R.id.homeFragment
        menu.findItem(R.id.aircraftFragment)?.isVisible = showMenu
        menu.findItem(R.id.settingsFragment)?.isVisible = showMenu
        menu.findItem(R.id.show_disclaimer)?.isVisible = showMenu
        menu.findItem(R.id.show_about)?.isVisible = showMenu
        menu.findItem(R.id.action_export_pdf)?.isVisible = showMenu
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
        val isWithinLimits = (totalMass <= (aircraft.maxTotalMass ?: Double.MAX_VALUE)) &&
                (cgLocationResult.value in (aircraft.minCg ?: Double.MIN_VALUE)..(aircraft.maxCg ?: Double.MAX_VALUE)) &&
                (nonLiftingMass <= (aircraft.maxNonLiftingMass ?: Double.MAX_VALUE))

        val range = (aircraft.maxCg ?: 0.0) - (aircraft.minCg ?: 0.0)
        val cgMac = if (range > 0) (cgLocationResult.value - (aircraft.minCg ?: 0.0)) / range * 100 else null

        val exporter = PdfExporter(this)
        val pdfFile = exporter.generateReport(profile, totalMass, totalMoment, nonLiftingMass, cgLocationResult.value, cgMac, isWithinLimits)

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
        } catch (e: Exception) { "N/A" }

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
        window.statusBarColor = Color.TRANSPARENT
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isDarkMode
        binding.appBarLayout.setBackgroundColor(primaryColor)
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}
