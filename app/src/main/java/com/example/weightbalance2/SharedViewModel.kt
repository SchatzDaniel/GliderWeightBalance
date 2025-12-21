// SharedViewModel.kt
package com.example.weightbalance2

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager

/**
 * Repräsentiert das Ergebnis einer Berechnung.
 * Kann entweder ein gültiger [Success]-Wert oder ein [Error] sein.
 */
sealed class CalculationResult {
    data class Success(val value: Double) : CalculationResult()
    object Error : CalculationResult()
}

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
    private val _totalMass = MutableLiveData<CalculationResult>()
    val totalMass: LiveData<CalculationResult> = _totalMass

    private val _cg = MutableLiveData<CalculationResult>()
    val cg: LiveData<CalculationResult> = _cg

    private val _nonLiftingMass = MutableLiveData<CalculationResult >()
    val nonLiftingMass: LiveData<CalculationResult  > = _nonLiftingMass

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

    // Haupt Berechnungsfunktion
    fun recalc() {
        // --- 1. Werte sammeln ---
        val mL = emptyMass.value ?: 0.0
        val mF = fuselageMass.value ?: 0.0
        val mS = stabilizerMass.value ?: 0.0

        // Massen und Arme aus dem ScrollingFragment
        val masses = persistentValues.map { it.key to getValue(it.key) }.toMap()
        val arms = mapOf(
            "pilot_mass" to (pilotArm.value ?: 0.0),
            "lower_baggage_mass" to (lowerBaggageArm.value ?: 0.0),
            "upper_baggage_mass" to (upperBaggageArm.value ?: 0.0),
            "trim_ballast_mass" to (trimBallastArm.value ?: 0.0),
            "water_ballast_mass" to (waterBallastArm.value ?: 0.0),
            "stabilizer_ballast_mass" to (stabilizerBallastArm.value ?: 0.0),
            "oxygen_mass" to (oxygenArm.value ?: 0.0),
            "instrument_mass" to (instrumentArm.value ?: 0.0),
            // Füge "trim_pillow_mass" und "parachute_mass" hinzu, die den Hebelarm des Piloten verwenden
            "trim_pillow_mass" to (pilotArm.value ?: 0.0),
            "cockpit_baggage_mass" to (pilotArm.value ?: 0.0),
            "parachute_mass" to (pilotArm.value ?: 0.0)
        )

        // --- 2. Fehlerprüfungen durchführen ---

        // Regel 1: Leermasse fehlt
        if (mL == 0.0) {
            _totalMass.value = CalculationResult.Error
            _cg.value = CalculationResult.Error
            _nonLiftingMass.value = CalculationResult.Error // Alle Ergebnisse betroffen
            return // Berechnung abbrechen
        }

        // Regel 2: Hebelarm fehlt für eine eingegebene Masse
        val hasCgInputError = (mL > 0.0 && (emptyArm.value ?: 0.0) == 0.0) || // PRÜFE AUCH DEN LEERMASSENHEBELARM
                masses.any { (key, mass) ->
                    mass > 0.0 && (arms[key] ?: 0.0) == 0.0
                }
        if (hasCgInputError) {
            _cg.value = CalculationResult.Error // Nur CG ist betroffen
        }

        // Regel 3: fuselageMass oder stabilizerMass fehlt
        val hasNonLiftingInputError = mF == 0.0 || mS == 0.0
        if (hasNonLiftingInputError) {
            _nonLiftingMass.value = CalculationResult.Error // Nur nonLiftingMass ist betroffen
        }

        // --- 3. Berechnungen durchführen ---

        // TotalMass (kann nicht mehr fehlschlagen, da wir mL > 0.0 geprüft haben)
        val mGes = mL + masses.values.sum()
        _totalMass.value = CalculationResult.Success(mGes)


        // CG (Schwerpunkt)
        if (!hasCgInputError) { // Nur berechnen, wenn kein Fehler vorliegt
            val totalMoment = (mL * (emptyArm.value ?: 0.0)) +
                    masses.entries.sumOf { (key, mass) -> mass * (arms[key] ?: 0.0) }

            if (mGes > 0) {
                _cg.value = CalculationResult.Success(totalMoment / mGes)
            } else {
                _cg.value = CalculationResult.Success(0.0)
            }
        }


        // Non-Lifting Mass
        if (!hasNonLiftingInputError) { // Nur berechnen, wenn kein Fehler vorliegt
            val nonLifting = mGes - mL - (masses["water_ballast_mass"] ?: 0.0) + mF + mS
            _nonLiftingMass.value = CalculationResult.Success(nonLifting)
        }
    }


    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(this)

        // Melde die Observer mit einer Schleife ab
        persistentValues.forEach { it.liveData.removeObserver(it.observer) }
    }
}
