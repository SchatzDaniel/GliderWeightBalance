package com.example.weightbalance2

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.navigation.navGraphViewModels
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}