// SharedViewModel.kt
package com.example.weightbalance2

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class SharedViewModel(application: Application) : AndroidViewModel(application),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    // LiveData für alle Werte. Diese werden von aussen (HomeFragment) beobachtet.
    private val _totalMass = MutableLiveData<Double>()
    val totalMass: LiveData<Double> = _totalMass

    private val _cg = MutableLiveData<Double>()
    val cg: LiveData<Double> = _cg

    private val _nonLiftingMass = MutableLiveData<Double>()
    val nonLiftingMass: LiveData<Double> = _nonLiftingMass

    // Eingabe-LiveData vom ScrollingFragment
    val pilotMass = MutableLiveData<Double>(0.0)
    val cockpitBaggageMass = MutableLiveData<Double>(0.0)
    val trimBallastMass = MutableLiveData<Double>(0.0)
    val trimPillowMass = MutableLiveData<Double>(0.0)
    val parachuteMass = MutableLiveData<Double>(0.0)
    val lowerBaggageMass = MutableLiveData<Double>(0.0)
    val upperBaggageMass = MutableLiveData<Double>(0.0)
    val waterBallastMass = MutableLiveData<Double>(0.0)
    val stabilizerBallastMass = MutableLiveData<Double>(0.0)
    val oxygenMass = MutableLiveData<Double>(0.0)
    val instrumentMass = MutableLiveData<Double>(0.0)

    // Interne LiveData für Werte aus den Preferences
    private val emptyMass = MutableLiveData<Double>()
    private val fuselageMass = MutableLiveData<Double>()
    private val stabilizerMass = MutableLiveData<Double>()

    private val emptyArm = MutableLiveData<Double>()
    private val pilotArm = MutableLiveData<Double>()
    private val lowerBaggageArm = MutableLiveData<Double>()
    private val upperBaggageArm = MutableLiveData<Double>()
    private val trimBallastArm = MutableLiveData<Double>()
    private val waterBallastArm = MutableLiveData<Double>()
    private val stabilizerBallastArm = MutableLiveData<Double>()
    private val oxygenArm = MutableLiveData<Double>()
    private val instrumentArm = MutableLiveData<Double>()

    init {
        // 1. Registriere den Listener
        prefs.registerOnSharedPreferenceChangeListener(this)

        // 2. Lade ALLE Werte EINMAL initial aus den SharedPreferences
        updateAllValuesFromPreferences()

        // 3. Führe die erste Berechnung durch
        recalc()
    }

    // Diese Methode wird bei JEDER Änderung an den Preferences aufgerufen
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        // Aktualisiere den spezifischen Wert, der sich geändert hat
        updateValueFromKey(key)
        // Führe eine Neuberechnung durch
        recalc()
    }

    // Hilfsmethode, um einen einzelnen Wert basierend auf dem Key zu aktualisieren
    private fun updateValueFromKey(key: String?) {
        when (key) {
            "empty_mass" -> emptyMass.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            "fuselage_mass" -> fuselageMass.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            "stabilizer_mass" -> stabilizerMass.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0

            "empty_arm" -> emptyArm.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            "pilot_arm" -> pilotArm.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            "lower_baggage_arm" -> lowerBaggageArm.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            "upper_baggage_arm" -> upperBaggageArm.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            "trim_ballast_arm" -> trimBallastArm.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            "water_ballast_arm" -> waterBallastArm.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            "stabilizer_ballast_arm" -> stabilizerBallastArm.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            "oxygen_arm" -> oxygenArm.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
            "instrument_arm" -> instrumentArm.value = prefs.getString(key, "0.0")
                ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        }
    }

    // Hilfsmethode, um alle Werte zu Beginn zu laden
    private fun updateAllValuesFromPreferences() {
        emptyMass.value = prefs.getString("empty_mass", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        fuselageMass.value = prefs.getString("fuselage_mass", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        stabilizerMass.value = prefs.getString("stabilizer_mass", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0

        emptyArm.value = prefs.getString("empty_arm", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        pilotArm.value = prefs.getString("pilot_arm", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        lowerBaggageArm.value = prefs.getString("lower_baggage_arm", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        upperBaggageArm.value = prefs.getString("upper_baggage_arm", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        trimBallastArm.value = prefs.getString("trim_ballast_arm", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        waterBallastArm.value = prefs.getString("water_ballast_arm", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        stabilizerBallastArm.value = prefs.getString("stabilizer_ballast_arm", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        oxygenArm.value = prefs.getString("oxygen_arm", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        instrumentArm.value = prefs.getString("instrument_arm", "0.0")
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
    }

    // Die zentrale Berechnungslogik, jetzt öffentlich, da wir sie manuell aufrufen
    fun recalc() {
        val mL = emptyMass.value ?: 0.0
        val mF = fuselageMass.value?: 0.0
        val mS = stabilizerMass.value?: 0.0
        val mP = listOfNotNull(
            pilotMass.value,
            trimPillowMass.value,
            cockpitBaggageMass.value,
            parachuteMass.value
        ).sum()
        val mLB = lowerBaggageMass.value ?: 0.0
        val mUB = upperBaggageMass.value ?: 0.0
        val mTB = trimBallastMass.value ?: 0.0
        val mWB = waterBallastMass.value ?: 0.0
        val mSB = stabilizerBallastMass.value ?: 0.0
        val mO = oxygenMass.value ?: 0.0
        val mI = instrumentMass.value ?: 0.0

        val xL = emptyArm.value ?: 0.0
        val xP = pilotArm.value ?: 0.0
        val xLB = lowerBaggageArm.value ?: 0.0
        val xUB = upperBaggageArm.value ?: 0.0
        val xTB = trimBallastArm.value ?: 0.0
        val xWB = waterBallastArm.value ?: 0.0
        val xSB = stabilizerBallastArm.value ?: 0.0
        val xO = oxygenArm.value ?: 0.0
        val xI = instrumentArm.value ?: 0.0

        val mGes = mL + mP + mLB + mUB + mTB + mWB + mSB + mO + mI

        if (mGes == 0.0) {
            _totalMass.value = 0.0
            _cg.value = 0.0
            return
        }

        val xSp = (mL * xL + mP * xP + mLB * xLB + mUB * xUB + mTB * xTB + mWB * xWB + mSB * xSB
                + mO * xO + mI * xI) / mGes

        _totalMass.value = mGes
        _cg.value = xSp
        _nonLiftingMass.value = mGes - mL + mF + mS
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}
