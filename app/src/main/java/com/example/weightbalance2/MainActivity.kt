package com.example.weightbalance2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.weightbalance2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Setzen Sie IHRE Toolbar als die primäre ActionBar.
        setSupportActionBar(binding.toolbar)

        // 2. Finde den NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 3. Erstelle die AppBarConfiguration
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        /// 4. Verbinde die jetzt offizielle ActionBar mit dem NavController.
        setupActionBarWithNavController(navController, appBarConfiguration)
    }
    // 5. Fügen Sie die onSupportNavigateUp-Methode wieder hinzu.
    //    Diese ist notwendig, wenn man setSupportActionBar verwendet.
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
