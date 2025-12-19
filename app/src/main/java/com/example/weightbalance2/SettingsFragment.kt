package com.example.weightbalance2

import android.os.Bundle
import androidx.navigation.navGraphViewModels
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    private val sharedViewModel: SharedViewModel by navGraphViewModels(R.id.main_nav)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}