// SharedViewModel.kt
package com.example.weightbalance2 // Use your app's package name

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager

class SharedViewModel(application: Application) : AndroidViewModel(application),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs =
        PreferenceManager.getDefaultSharedPreferences(application)

    // Lade die Keys aus der XML-Ressource und konvertiere sie in ein Set für performante Abfragen.
    private val recalcKeys: Set<String> by lazy {
        application.resources.getStringArray(R.array.recalculation_preference_keys).toSet()
    }

    val pilotMass = MutableLiveData<Double>()

    val totalMass = MediatorLiveData<Double>()
    val cg = MediatorLiveData<Double>()

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)

        totalMass.addSource(pilotMass) { recalc() }
        cg.addSource(pilotMass) { recalc() }
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String?) // Der Key kann nullable sein, das sollten wir berücksichtigen.
    {
        // Prüfe, ob der geänderte Key in unserem Set von Keys enthalten ist.
        if (key in recalcKeys) {
            recalc()
        }
    }

    private fun recalc() {
        // Diese Zeile sorgt dafür, dass die Neuberechnung nur stattfindet,
        // wenn pilotMass bereits einen gültigen Wert hat.
        val mP = pilotMass.value ?: return

        // Hole die Werte aus den SharedPreferences.
        val mL = prefs.getFloat("Leermasse", 0f).toDouble()
        val xL = prefs.getFloat("LeermasseHebel", 0f).toDouble()
        val xP = prefs.getFloat("PilotHebel", 0f).toDouble()

        val mGes = mL + mP

        // Füge eine Sicherheitsprüfung hinzu, um eine Division durch Null zu vermeiden.
        if (mGes == 0.0) {
            totalMass.value = 0.0
            cg.value = 0.0
            return
        }

        val xSp = (mL * xL + mP * xP) / mGes

        totalMass.value = mGes
        cg.value = xSp
    }

    override fun onCleared() {
        super.onCleared() // Es ist eine gute Praxis, super.onCleared() aufzurufen.
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}
