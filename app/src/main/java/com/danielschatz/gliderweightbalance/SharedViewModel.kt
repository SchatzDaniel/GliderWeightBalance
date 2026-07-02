package com.danielschatz.gliderweightbalance

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.danielschatz.gliderweightbalance.data.dao.PayloadStationDao
import com.danielschatz.gliderweightbalance.data.database.AppDatabase
import com.danielschatz.gliderweightbalance.data.model.AircraftProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * Repräsentiert das Ergebnis einer Berechnung.
 */
sealed class CalculationResult {
    data class Success(val value: Double) : CalculationResult()
    data object Error : CalculationResult()
}

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    private val aircraftProfileDao = AppDatabase.getDatabase(application).aircraftProfileDao()
    private val payloadStationDao: PayloadStationDao = AppDatabase.getDatabase(application).payloadStationDao()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    // 1. Das ausgewählte Flugzeugprofil. Dies ist jetzt die "Single Source of Truth".
    private val _selectedProfile = MediatorLiveData<AircraftProfile?>()
    val selectedProfile: LiveData<AircraftProfile?> = _selectedProfile

    // 2. Die Massen, die der Benutzer eingibt.
    private val _stationMasses = MutableLiveData<Map<Int, Double>>() // Map<stationId, mass>
    
    // Gibt an, ob die aktuelle Änderung animiert werden soll (z.B. bei Slider = false)
    private val _shouldAnimate = MutableLiveData<Boolean>(true)
    val shouldAnimate: LiveData<Boolean> = _shouldAnimate

    // --- BERECHNUNGSERGEBNISSE (BLEIBEN GLEICH) ---
    private val _totalMass = MutableLiveData<CalculationResult>()
    val totalMass: LiveData<CalculationResult> = _totalMass

    private val _cg = MutableLiveData<CalculationResult>()
    val cg: LiveData<CalculationResult> = _cg

    private val _cgRange = MutableLiveData<Pair<Double, Double>?>()
    val cgRange: LiveData<Pair<Double, Double>?> = _cgRange

    // NonLiftingMass wird vereinfacht, da es nicht mehr aus festen Feldern berechnet wird.
    private val _nonLiftingMass = MutableLiveData<CalculationResult>()
    val nonLiftingMass: LiveData<CalculationResult > = _nonLiftingMass

    // Hält die aktuelle Höhe des Dashboards für das ScrollingFragment bereit
    private val _headerHeight = MutableLiveData<Int>()
    val headerHeight: LiveData<Int> = _headerHeight

    fun setHeaderHeight(height: Int) {
        _headerHeight.value = height
    }

    init {
        // Lade das zuletzt ausgewählte Flugzeug beim Start der App
        loadInitialAircraft()

        // Registriere die Beobachter, die eine Neuberechnung auslösen
        _selectedProfile.observeForever { recalc() }
        _stationMasses.observeForever { recalc() }
    }

    /**
     * Lädt das Profil, das beim letzten Mal ausgewählt war.
     */
    private fun loadInitialAircraft() {
        val lastId = prefs.getInt("last_selected_aircraft_id", R.integer.default_aircraft_id)
        if (lastId != R.integer.default_aircraft_id) {
            // Lade das Profil aus der DB.
            val source = aircraftProfileDao.getProfileById(lastId).asLiveData()
            _selectedProfile.addSource(source) { profile ->
                // Sobald das Profil geladen ist, setze es als ausgewählt.
                _selectedProfile.value = profile

                // stationMasses initialisieren, damit recalc() beim App-Start Werte zum Rechnen hat
                if (profile != null) {
                    val initialMasses = profile.stations.asSequence()
                        .filter { it.station.defaultValue != null }
                        .associateBy({ it.station.stationId }, { it.station.defaultValue ?: 0.0 })
                    _stationMasses.value = initialMasses
                }
                // Entferne die Quelle, um Speicherlecks zu vermeiden.
                _selectedProfile.removeSource(source)
            }
        }
    }

    /**
     * Wird vom AircraftFragment aufgerufen, wenn der Benutzer ein Flugzeug auswählt.
     */
    fun selectProfile(profile: AircraftProfile?) {
        if (profile == null) {
            _selectedProfile.value = null
            prefs.edit { putInt("last_selected_aircraft_id", -1) }
            _stationMasses.value = emptyMap()
            return
        }
        
        // ID speichern
        prefs.edit { putInt("last_selected_aircraft_id", profile.aircraft.id) }

        // Wir laden das Profil REACTIVE aus der Datenbank.
        // Falls das übergebene Profil neu gespeichert wurde (ID=0), 
        // wird es hier ignoriert und wir warten auf den echten DB-Eintrag.
        if (profile.aircraft.id > 0) {
            val source = aircraftProfileDao.getProfileById(profile.aircraft.id).asLiveData()
            _selectedProfile.addSource(source) { updatedProfile ->
                if (updatedProfile != null) {
                    _selectedProfile.value = updatedProfile
                    
                    // Massen nur initialisieren, wenn sie noch nicht da sind oder das Flugzeug wechselt
                    val initialMasses = updatedProfile.stations.asSequence()
                        .filter { it.station.defaultValue != null }
                        .associateBy({ it.station.stationId }, { it.station.defaultValue ?: 0.0 })
                    _stationMasses.value = initialMasses
                }
                _selectedProfile.removeSource(source)
            }
        } else {
            // Fallback für temporäre Profile (sollte im Normalfall nicht passieren)
            _selectedProfile.value = profile
        }
    }

    /**
     * Selektiert ein Profil anhand seiner ID.
     */
    fun selectProfileById(id: Int) {
        if (id == -1 || id == R.integer.default_aircraft_id) {
            selectProfile(null)
            return
        }

        val source = aircraftProfileDao.getProfileById(id).asLiveData()
        _selectedProfile.addSource(source) { profile ->
            if (profile != null) {
                selectProfile(profile)
            }
            _selectedProfile.removeSource(source)
        }
    }

    /**
     * Speichert den exakten Zustand einer Station in der Datenbank.
     */
    fun updateStationState(stationId: Int, mass: Double, presetLabel: String?, amount: Int, isSlider: Boolean = false) {
        _shouldAnimate.value = !isSlider // Animation nur, wenn es KEIN Slider ist

        viewModelScope.launch(Dispatchers.IO) {
            // 1. In der Datenbank aktualisieren
            payloadStationDao.updateStationState(stationId, mass, presetLabel, amount)

            // 2. Den lokalen State im UI-Thread aktualisieren (für die Live-Berechnung)
            launch(Dispatchers.Main) {
                val currentMasses = _stationMasses.value?.toMutableMap() ?: mutableMapOf()
                currentMasses[stationId] = mass
                _stationMasses.value = currentMasses

                // Wir müssen auch das Profil aktualisieren, damit der Adapter beim Scrollen
                // die neuen Werte aus den Stations-Objekten liest.
                _selectedProfile.value?.let { profile ->
                    profile.stations.find { it.station.stationId == stationId }?.let {
                        it.station.defaultValue = mass
                        it.station.selectedPresetLabel = presetLabel
                        it.station.amount = amount
                    }
                }
            }
        }
    }

    // Ein spezielles Signal für den Adapter, dass nur Werte aktualisiert wurden
    private val _onScenarioApplied = MutableLiveData<Unit>()
    val onScenarioApplied: LiveData<Unit> = _onScenarioApplied

    fun applyScenarioEntries(entries: List<com.danielschatz.gliderweightbalance.data.model.ScenarioEntry>) {
        _shouldAnimate.value = true // Konfigurationen immer animieren
        viewModelScope.launch(Dispatchers.IO) {
            // 1. In der Datenbank aktualisieren
            entries.forEach { entry ->
                payloadStationDao.updateStationState(entry.stationId, entry.value ?: 0.0, entry.selectedPresetLabel, entry.amount)
            }
            
            // 2. UI im Main-Thread aktualisieren
            launch(Dispatchers.Main) {
                _selectedProfile.value?.let { profile ->
                    val currentMasses = _stationMasses.value?.toMutableMap() ?: mutableMapOf()
                    
                    entries.forEach { entry ->
                        currentMasses[entry.stationId] = entry.value ?: 0.0
                        profile.stations.find { it.station.stationId == entry.stationId }?.let {
                            it.station.defaultValue = entry.value
                            it.station.selectedPresetLabel = entry.selectedPresetLabel
                            it.station.amount = entry.amount
                        }
                    }
                    
                    _stationMasses.value = currentMasses
                    // Wir triggern das Profil UND das spezielle Event
                    _selectedProfile.value = profile
                    _onScenarioApplied.value = Unit
                }
            }
        }
    }

    /**
     * Die NEUE, VIEL EINFACHERE Berechnungsfunktion.
     */
    private fun recalc() {
        val profile = _selectedProfile.value
        val masses = _stationMasses.value ?: emptyMap()

        // SCHUTZBEDINGUNG: Wenn kein Flugzeug da ist, gibt es nichts zu berechnen.
        if (profile == null || profile.aircraft.emptyWeight == null) {
            _totalMass.value = CalculationResult.Error
            _cg.value = CalculationResult.Error
            _nonLiftingMass.value = CalculationResult.Error
            return
        }

        // --- 1. TotalMass berechnen ---
        // Wir müssen hier die Dichte berücksichtigen, falls eine Station in Litern angegeben ist.
        val totalPayloadMass = profile.stations.sumOf { swp ->
            val rawValue = masses[swp.station.stationId] ?: 0.0
            val density = getDensity(swp.station.fluidType)
            rawValue * density
        }

        val emptyWeight = profile.aircraft.emptyWeight!!
        val mGes = emptyWeight + totalPayloadMass
        _totalMass.value = CalculationResult.Success(mGes)

        // --- 2. Schwerpunkt (CG) berechnen ---
        val emptyWeightArm = profile.aircraft.emptyWeightArm
        if (emptyWeightArm == null) {
            _cg.value = CalculationResult.Error // Fehler, wenn Leermassen-Hebelarm fehlt
            _cgRange.value = null
        } else {
            // 2a. Aktueller CG (Take-off)
            val payloadMoment = profile.sortedStations.sumOf { station ->
                val rawValue = masses[station.stationId] ?: 0.0
                val density = getDensity(station.fluidType)
                val mass = rawValue * density
                mass * station.arm
            }
            val takeOffMoment = (emptyWeight * emptyWeightArm) + payloadMoment
            if (mGes > 0) {
                _cg.value = CalculationResult.Success(takeOffMoment / mGes)
            } else {
                _cg.value = CalculationResult.Success(0.0)
            }

            // 2b. CG Range (Physikalische Simulation) berechnen
            val consumableStations = profile.stations.filter { it.station.isConsumable }
            
            if (consumableStations.isEmpty()) {
                _cgRange.value = null
            } else {
                // 1. Gruppiere Stationen in "Ablass-Ventile"
                // Jede Gruppe (ID > 0) ist ein Ventil. Jede einzelne Station (ID == 0) ist ein Ventil.
                val coupledValves = consumableStations.filter { it.station.couplingGroupId != 0 }
                    .groupBy { it.station.couplingGroupId }
                    .values.toList()
                
                val independentValves = consumableStations.filter { it.station.couplingGroupId == 0 }
                    .map { listOf(it) }
                
                val allValves = coupledValves + independentValves
                
                val results = mutableListOf<Double>()
                // Aktuellen Zustand als Startpunkt hinzufügen
                if (mGes > 0) results.add(takeOffMoment / mGes)

                // 2. Simuliere alle Kombinationen von geöffneten Ventilen
                val numValveCombos = 1 shl allValves.size
                
                for (i in 1 until numValveCombos) {
                    val activeValves = allValves.filterIndexed { index, _ -> (i shr index) and 1 == 1 }
                    val activeStations = activeValves.flatten()
                    
                    // Vorbereiten der Startparameter für alle aktiven Tanks
                    val stationSims = activeStations.mapNotNull { swp ->
                        val currentMass = (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
                        val maxMass = (swp.station.maxMass ?: 0.0) * getDensity(swp.station.fluidType)
                        val dumpTime = swp.station.dumpTime ?: 0.0
                        
                        if (currentMass > 0 && dumpTime > 0 && maxMass > 0) {
                            val tStart = dumpTime * (1.0 - sqrt(currentMass / maxMass))
                            Triple(swp, tStart, dumpTime)
                        } else null
                    }

                    if (stationSims.isEmpty()) continue

                    // Simulation über die Zeit (Sekundenschritte)
                    val maxDuration = stationSims.maxOf { it.third - it.second }
                    
                    for (dt in 0..maxDuration.toInt() step 1) {
                        var comboMass = emptyWeight
                        var comboMoment = emptyWeight * emptyWeightArm

                        // Unveränderbare Zuladung
                        profile.stations.filter { !it.station.isConsumable }.forEach { swp ->
                            val m = (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
                            comboMass += m
                            comboMoment += m * swp.station.arm
                        }

                        // Veränderbare Zuladung (nicht aktive Ventile bleiben voll)
                        consumableStations.forEach { swp ->
                            val sim = stationSims.find { it.first.station.stationId == swp.station.stationId }
                            val m = if (sim != null) {
                                // Berechne Restmasse nach Torricelli
                                val t = (sim.second + dt).coerceAtMost(sim.third)
                                val maxM = (swp.station.maxMass ?: 0.0) * getDensity(swp.station.fluidType)
                                maxM * (1.0 - t / sim.third).pow(2.0)
                            } else {
                                // Ventil zu -> aktuelle Masse bleibt
                                (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
                            }
                            comboMass += m
                            comboMoment += m * swp.station.arm
                        }

                        if (comboMass > 0) {
                            results.add(comboMoment / comboMass)
                        }
                    }
                    
                    // Endzustand (alle aktiven Tanks leer) explizit hinzufügen
                    var finalMass = emptyWeight
                    var finalMoment = emptyWeight * emptyWeightArm
                    profile.stations.forEach { swp ->
                        if (!swp.station.isConsumable || !activeStations.contains(swp)) {
                            val m = (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
                            finalMass += m
                            finalMoment += m * swp.station.arm
                        }
                    }
                    if (finalMass > 0) results.add(finalMoment / finalMass)
                }
                
                if (results.isNotEmpty()) {
                    _cgRange.value = Pair(results.min(), results.max())
                } else {
                    _cgRange.value = null
                }
            }
        }

        // --- 3. Non-Lifting Mass (Dynamische Berechnung) ---
        // Grundlage: Leermasse des Rumpfes/Leitwerks + alle markierten Stationen
        val emptyFuselage = profile.aircraft.fuselageMass ?: 0.0
        val emptyStabilizer = profile.aircraft.stabilizerMass ?: 0.0

        // Berechne die Summe der Massen aller Stationen, die als "Non-Lifting" markiert sind
        val payloadNonLiftingMass = profile.stations.sumOf { swp ->
            if (swp.station.isNonLifting) {
                val rawValue = masses[swp.station.stationId] ?: 0.0
                val density = getDensity(swp.station.fluidType)
                rawValue * density
            } else {
                0.0
            }
        }

        val totalNonLiftingMass = emptyFuselage + emptyStabilizer + payloadNonLiftingMass

        if (totalNonLiftingMass > 0) {
            _nonLiftingMass.value = CalculationResult.Success(totalNonLiftingMass)
        } else {
            _nonLiftingMass.value = CalculationResult.Error
        }
    }

    private fun getDensity(fluidType: String?): Double {
        return when (fluidType) {
            "WATER" -> 1.0
            "GASOLINE" -> 0.72
            "KEROSENE" -> 0.80
            else -> 1.0
        }
    }
}
