// SharedViewModel.kt
package com.example.weightbalance2

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager

class SharedViewModel(application: Application) : AndroidViewModel(application),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    private val persistentValues = listOf(
        PersistentValue(
            key = "pilot_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("pilot_mass", value.toString()).apply() }
        ),
        PersistentValue(
            key = "cockpit_baggage_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("cockpit_baggage_mass", value.toString()).apply() }
        ),
        PersistentValue(
            key = "trim_ballast_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("trim_ballast_mass", value.toString()).apply() }
        ),
        PersistentValue(
            key = "trim_pillow_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("trim_pillow_mass", value.toString()).apply() }
        ),
        PersistentValue(
            key = "parachute_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("parachute_mass", value.toString()).apply() }
        ),
        PersistentValue(
            key = "lower_baggage_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("lower_baggage_mass", value.toString()).apply() }
        ),
        PersistentValue(
            key = "upper_baggage_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("upper_baggage_mass", value.toString()).apply() }
        ),
        PersistentValue(
            key = "water_ballast_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("water_ballast_mass", value.toString()).apply() }
        ),
        PersistentValue(
            key = "stabilizer_ballast_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("stabilizer_ballast_mass", value.toString()).apply() }
        ),
        PersistentValue(
            key = "oxygen_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("oxygen_mass", value.toString()).apply() }
        ),
        PersistentValue(
            key = "instrument_mass",
            liveData = MutableLiveData(0.0),
            observer = Observer { value ->
                prefs.edit().putString("instrument_mass", value.toString()).apply() }
        )
    )



    // BESTEHENDE KEYS AUS root_preferences.xml
    private val KEY_MAS_TOTAL_MASS = "max_total_mass"
    private val KEY_MAX_NON_LIFTING_MASS = "max_non_lifting_mass"
    private val KEY_MIN_CG = "min_cg"
    private val KEY_MAX_CG = "max_cg"

    private val KEY_EMPTY_MASS = "empty_mass"
    private val KEY_FUSELAGE_MASS = "fuselage_mass"
    private val KEY_STABILIZER_MASS = "stabilizer_mass"

    private val KEY_EMPTY_ARM = "empty_arm"
    private val KEY_PILOT_ARM = "pilot_arm"
    private val KEY_LB_ARM = "lower_baggage_arm"
    private val KEY_UB_ARM = "upper_baggage_arm"
    private val KEY_TRIM_BALLAST_ARM = "trim_ballast_arm"
    private val KEY_WATER_ARM = "water_ballast_arm"
    private val KEY_STABILIZER_BALLAST_ARM = "stabilizer_ballast_arm"
    private val KEY_OXYGEN_ARM = "oxygen_arm"
    private val KEY_INSTRUMENT_ARM = "instrument_arm"

    // LiveData für alle Werte. Diese werden von aussen (HomeFragment) beobachtet.
    private val _totalMass = MutableLiveData<Double>()
    val totalMass: LiveData<Double> = _totalMass

    private val _cg = MutableLiveData<Double>()
    val cg: LiveData<Double> = _cg

    private val _nonLiftingMass = MutableLiveData<Double>()
    val nonLiftingMass: LiveData<Double> = _nonLiftingMass

    // Eingabe-LiveData vom ScrollingFragment
    val pilotMass = persistentValues.first { it.key == "pilot_mass" }.liveData
    val cockpitBaggageMass = persistentValues.first { it.key == "cockpit_baggage_mass" }.liveData
    val trimBallastMass = persistentValues.first { it.key == "trim_ballast_mass" }.liveData
    val trimPillowMass = persistentValues.first { it.key == "trim_pillow_mass" }.liveData
    val parachuteMass = persistentValues.first { it.key == "parachute_mass" }.liveData
    val lowerBaggageMass = persistentValues.first { it.key == "lower_baggage_mass" }.liveData
    val upperBaggageMass = persistentValues.first { it.key == "upper_baggage_mass" }.liveData
    val waterBallastMass = persistentValues.first { it.key == "water_ballast_mass" }.liveData
    val stabilizerBallastMass = persistentValues.first { it.key == "stabilizer_ballast_mass" }.liveData
    val oxygenMass = persistentValues.first { it.key == "oxygen_mass" }.liveData
    val instrumentMass = persistentValues.first { it.key == "instrument_mass" }.liveData

    private fun getLiveData(key: String): LiveData<Double> =
        persistentValues.first { it.key == key }.liveData

    // Interne LiveData für Werte aus den Preferences
    val maxTotalMass = MutableLiveData<Double>()
    val maxNonLiftingMass = MutableLiveData<Double>()
    val minCG = MutableLiveData<Double>()
    val maxCG = MutableLiveData<Double>()

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

    private data class PersistentValue(
        val key: String,
        val liveData: MutableLiveData<Double>,
        val observer: Observer<Double>
    )

    init {
        // 1. Registriere den Listener
        prefs.registerOnSharedPreferenceChangeListener(this)

        // 2. Lade ALLE Werte EINMAL initial aus den SharedPreferences
        updateAllValuesFromPreferences()

        // 3. Führe die erste Berechnung durch
        recalc()

        // 4. Registriere die Observer mit einer Schleife
        persistentValues.forEach { it.liveData.observeForever(it.observer) }
    }

    // Diese Methode wird bei JEDER Änderung an den Preferences aufgerufen
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        // Finde heraus, ob dieser Key von einem unserer "ScrollingFragment"-Werte stammt.
        val isPersistentValueKey = persistentValues.any { it.key == key }
        if (isPersistentValueKey){
            return
        }
        // Aktualisiere den spezifischen Wert, der sich geändert hat
        updateValueFromKey(key)
        // Führe eine Neuberechnung durch
        recalc()
    }

    // Hilfsmethode, um einen einzelnen Wert basierend auf dem Key zu aktualisieren
    private fun updateValueFromKey(key: String?) {
        val newValue =
            prefs.getString(key, "0.0")?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        when (key) {
            KEY_MAS_TOTAL_MASS -> maxTotalMass.value = newValue
            KEY_MAX_NON_LIFTING_MASS -> maxNonLiftingMass.value = newValue
            KEY_MIN_CG -> minCG.value = newValue
            KEY_MAX_CG -> maxCG.value = newValue

            KEY_EMPTY_MASS -> emptyMass.value = newValue
            KEY_FUSELAGE_MASS -> fuselageMass.value = newValue
            KEY_STABILIZER_MASS -> stabilizerMass.value = newValue

            KEY_EMPTY_ARM -> emptyArm.value = newValue
            KEY_PILOT_ARM -> pilotArm.value = newValue
            KEY_LB_ARM -> lowerBaggageArm.value = newValue
            KEY_UB_ARM -> upperBaggageArm.value = newValue
            KEY_TRIM_BALLAST_ARM -> trimBallastArm.value = newValue
            KEY_WATER_ARM -> waterBallastArm.value = newValue
            KEY_STABILIZER_BALLAST_ARM -> stabilizerBallastArm.value = newValue
            KEY_OXYGEN_ARM -> oxygenArm.value = newValue
            KEY_INSTRUMENT_ARM -> instrumentArm.value = newValue
        }
    }

    // Hilfsmethode, um alle Werte zu Beginn zu laden
    private fun updateAllValuesFromPreferences() {
        fun readStringAsDouble(key: String): Double {
            return prefs.getString(key, "0.0")?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        }
        maxTotalMass.value = readStringAsDouble(KEY_MAS_TOTAL_MASS)
        maxNonLiftingMass.value = readStringAsDouble(KEY_MAX_NON_LIFTING_MASS)
        minCG.value = readStringAsDouble(KEY_MIN_CG)
        maxCG.value = readStringAsDouble(KEY_MAX_CG)

        emptyMass.value = readStringAsDouble(KEY_EMPTY_MASS)
        fuselageMass.value = readStringAsDouble(KEY_FUSELAGE_MASS)
        stabilizerMass.value = readStringAsDouble(KEY_STABILIZER_MASS)

        emptyArm.value = readStringAsDouble(KEY_EMPTY_ARM)
        pilotArm.value = readStringAsDouble(KEY_PILOT_ARM)
        lowerBaggageArm.value = readStringAsDouble(KEY_LB_ARM)
        upperBaggageArm.value = readStringAsDouble(KEY_UB_ARM)
        trimBallastArm.value = readStringAsDouble(KEY_TRIM_BALLAST_ARM)
        waterBallastArm.value = readStringAsDouble(KEY_WATER_ARM)
        stabilizerBallastArm.value = readStringAsDouble(KEY_STABILIZER_BALLAST_ARM)
        oxygenArm.value = readStringAsDouble(KEY_OXYGEN_ARM)
        instrumentArm.value = readStringAsDouble(KEY_INSTRUMENT_ARM)

        persistentValues.forEach { value ->
            value.liveData.value = prefs.getString(value.key, "0.0")?.toDoubleOrNull() ?: 0.0
        }
    }

    // Hilfsfunktion für recalc
    private fun getValue(key: String): Double = persistentValues.first { it.key == key }.liveData.value ?: 0.0

    // Die zentrale Berechnungslogik, jetzt öffentlich, da wir sie manuell aufrufen
    fun recalc() {
        val mL = emptyMass.value ?: 0.0
        val mF = fuselageMass.value?: 0.0
        val mS = stabilizerMass.value?: 0.0
        val mP = listOfNotNull(
            getValue("pilot_mass"),
            getValue("trim_pillow_mass"),
            getValue("cockpit_baggage_mass"),
            getValue("parachute_mass")
        ).sum()
        val mLB = getValue("lower_baggage_mass")
        val mUB = getValue("upper_baggage_mass")
        val mTB = getValue("trim_ballast_mass")
        val mWB = getValue("water_ballast_mass")
        val mSB = getValue("stabilizer_ballast_mass")
        val mO = getValue("oxygen_mass")
        val mI = getValue("instrument_mass")

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

        // Melde die Observer mit einer Schleife ab
        persistentValues.forEach { it.liveData.removeObserver(it.observer) }
    }
}
