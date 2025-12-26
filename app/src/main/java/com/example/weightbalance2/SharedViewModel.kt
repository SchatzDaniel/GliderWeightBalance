// SharedViewModel.kt
package com.example.weightbalance2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.example.weightbalance2.data.model.Aircraft
import androidx.core.content.edit

/**
 * Repräsentiert das Ergebnis einer Berechnung.
 * Kann entweder ein gültiger [Success]-Wert oder ein [Error] sein.
 */
sealed class CalculationResult {
    data class Success(val value: Double) : CalculationResult()
    object Error : CalculationResult()
}

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedAircraft = MutableLiveData<Aircraft?>()
    val selectedAircraft: LiveData<Aircraft?> get() = _selectedAircraft

    /**
     * Setzt das ausgewählte Flugzeug. Diese Methode wird vom AircraftFragment aufgerufen.
     * @param aircraft Das vom Benutzer ausgewählte Flugzeug.
     */
    fun selectAircraft(aircraft: Aircraft) {
        _selectedAircraft.value = aircraft
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    private val persistentValues = listOf(
        PersistentValue(
            key = "pilot_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("pilot_mass", value.toString()) } }
        ),
        PersistentValue(
            key = "cockpit_baggage_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("cockpit_baggage_mass", value.toString()) } }
        ),
        PersistentValue(
            key = "trim_ballast_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("trim_ballast_mass", value.toString()) } }
        ),
        PersistentValue(
            key = "trim_pillow_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("trim_pillow_mass", value.toString()) } }
        ),
        PersistentValue(
            key = "parachute_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("parachute_mass", value.toString()) } }
        ),
        PersistentValue(
            key = "lower_baggage_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("lower_baggage_mass", value.toString()) } }
        ),
        PersistentValue(
            key = "upper_baggage_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("upper_baggage_mass", value.toString()) } }
        ),
        PersistentValue(
            key = "water_ballast_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("water_ballast_mass", value.toString()) } }
        ),
        PersistentValue(
            key = "stabilizer_ballast_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("stabilizer_ballast_mass", value.toString()) } }
        ),
        PersistentValue(
            key = "oxygen_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("oxygen_mass", value.toString()) } }
        ),
        PersistentValue(
            key = "instrument_mass",
            liveData = MutableLiveData(0.0),
            observer = { value ->
                prefs.edit { putString("instrument_mass", value.toString()) } }
        )
    )

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

    // LiveData für alle Berechnungsergebnisse. Diese werden von aussen (HomeFragment) beobachtet.
    private val _totalMass = MutableLiveData<CalculationResult>()
    val totalMass: LiveData<CalculationResult> = _totalMass

    private val _cg = MutableLiveData<CalculationResult>()
    val cg: LiveData<CalculationResult> = _cg

    private val _nonLiftingMass = MutableLiveData<CalculationResult >()
    val nonLiftingMass: LiveData<CalculationResult  > = _nonLiftingMass

    private fun getLiveData(key: String): LiveData<Double> =
        persistentValues.first { it.key == key }.liveData

    // Die Definition von PersistentValue bleibt, aber sie ist jetzt nur für Massen zuständig
    // kann später nützlich sein, zb. für Massenpresets
    private data class PersistentValue(
        val key: String,
        val liveData: MutableLiveData<Double>,
        val observer: Observer<Double>
    )

    init {
        // 1. Initialisiere alle persistenten Werte (Benutzereingaben) in einer einzigen Schleife.
        persistentValues.forEach { persistentValue ->
            // a) Lade den zuletzt gespeicherten Wert aus den SharedPreferences.
            persistentValue.liveData.value = prefs.getString(persistentValue.key, "0.0")?.toDoubleOrNull() ?: 0.0

            // b) Registriere den Observer, der bei Änderung den neuen Wert sofort speichert.
            persistentValue.liveData.observeForever(persistentValue.observer)

            // c) Registriere einen weiteren Observer, der bei Änderung die Neuberechnung anstößt.
            persistentValue.liveData.observeForever {
                recalc()
            }
        }

        // 2. Beobachte das ausgewählte Flugzeug. Dies ist der zweite zentrale Trigger.
        selectedAircraft.observeForever { aircraft ->
            // Wenn sich das Flugzeug ändert (oder zum ersten Mal gesetzt wird),
            // reicht ein einfacher Aufruf von recalc().
            // Die Funktion ist schlau genug, um zu wissen, was zu tun ist.
            recalc()
        }
    }


    // Hilfsfunktion für recalc
    private fun getValue(key: String): Double = persistentValues.first { it.key == key }.liveData.value ?: 0.0

    // Haupt Berechnungsfunktion
    fun recalc() {
        val aircraft = selectedAircraft.value
        // SCHUTZBEDINGUNG: Breche sofort ab, wenn kein Flugzeug ausgewählt ist.
        // In diesem Fall sind die Basisdaten (Leermasse, Hebelarme) nicht vorhanden.
        if (aircraft == null) {
            // Setze auf definierte Standardwerte, anstatt einen Fehler zu erzeugen.
            _totalMass.value = CalculationResult.Success(0.0)
            _cg.value = CalculationResult.Success(0.0)
            _nonLiftingMass.value = CalculationResult.Success(0.0)
            return
        }

        // --- 1. Werte sammeln ---

        // Massen und Arme aus dem ScrollingFragment
        val masses = persistentValues.map { it.key to getValue(it.key) }.toMap()
        val arms = mapOf(
            "pilot_mass" to aircraft.pilotMassArm,
            "lower_baggage_mass" to aircraft.lowerBaggageMassArm,
            "upper_baggage_mass" to aircraft.upperBaggageMassArm,
            "trim_ballast_mass" to aircraft.trimBallastMassArm,
            "water_ballast_mass" to aircraft.waterBallastMassArm,
            "stabilizer_ballast_mass" to aircraft.stabilizerBallastMassArm,
            "oxygen_mass" to aircraft.oxygenMassArm,
            "instrument_mass" to aircraft.instrumentMassArm,
            // Füge "trim_pillow_mass" und "parachute_mass" hinzu, die den Hebelarm des Piloten verwenden
            "trim_pillow_mass" to aircraft.pilotMassArm,
            "cockpit_baggage_mass" to aircraft.pilotMassArm,
            "parachute_mass" to aircraft.pilotMassArm
        )

        // --- 2. Fehlerprüfungen durchführen ---

        // Regel 1: Leermasse fehlt
        if (aircraft.emptyWeight == null) {
            _totalMass.value = CalculationResult.Error
            _cg.value = CalculationResult.Error
            _nonLiftingMass.value = CalculationResult.Error // Alle Ergebnisse betroffen
            return // Berechnung abbrechen
        }

        // Regel 2: Hebelarm fehlt für eine eingegebene Masse
        val hasCgInputError = (aircraft.emptyWeight > 0.0 && aircraft.emptyWeightArm == null) || // PRÜFE AUCH DEN LEERMASSENHEBELARM
                masses.any { (key, mass) -> mass > 0.0 && arms[key] == null }

        // Regel 3: fuselageMass oder stabilizerMass fehlt
        val hasNonLiftingInputError = aircraft.fuselageMass == null || aircraft.stabilizerMass == null
        if (hasNonLiftingInputError) {
            _nonLiftingMass.value = CalculationResult.Error // Nur nonLiftingMass ist betroffen
        }

        // --- 3. Berechnungen durchführen ---

        // TotalMass
        val mGes = aircraft.emptyWeight + masses.values.sum()
        _totalMass.value = CalculationResult.Success(mGes)

        // CG (Schwerpunkt)
        if (hasCgInputError) {
            _cg.value = CalculationResult.Error
        } else {
            // Wenn 'emptyWeightArm' nicht null ist, kann die Berechnung sicher stattfinden
            val emptyArm = aircraft.emptyWeightArm
            if (emptyArm != null) {
                val totalMoment = (aircraft.emptyWeight * emptyArm) + // Kein "!!" mehr nötig
                        masses.entries.sumOf { (key, mass) -> mass * (arms[key] ?: 0.0) }

                if (mGes > 0) {
                    _cg.value = CalculationResult.Success(totalMoment / mGes)
                } else {
                    _cg.value = CalculationResult.Success(0.0)
                }
            } else {
                // Dieser Fall sollte durch 'hasCgInputError' abgedeckt sein,
                // ist aber eine zusätzliche Sicherheitsschicht.
                _cg.value = CalculationResult.Error
            }
        }

        // Non-Lifting Mass
        if (hasNonLiftingInputError) {
            _nonLiftingMass.value = CalculationResult.Error
        } else {
            // Smart-Cast: Der Compiler weiß innerhalb dieses Blocks,
            // dass die Werte nicht null sind.
            val fuselageMass = aircraft.fuselageMass
            val stabilizerMass = aircraft.stabilizerMass
            if (fuselageMass != null && stabilizerMass != null) {
                val currentWaterMass = masses["water_ballast_mass"] ?: 0.0
                val nonLifting = mGes - aircraft.emptyWeight -
                        currentWaterMass + fuselageMass + stabilizerMass
                _nonLiftingMass.value = CalculationResult.Success(nonLifting)
            } else {
                _nonLiftingMass.value = CalculationResult.Error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        persistentValues.forEach { it.liveData.removeObserver(it.observer) }
    }
}