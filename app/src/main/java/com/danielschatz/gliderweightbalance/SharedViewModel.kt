package com.danielschatz.gliderweightbalance

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.danielschatz.gliderweightbalance.data.dao.PayloadStationDao
import com.danielschatz.gliderweightbalance.data.database.AppDatabase
import com.danielschatz.gliderweightbalance.data.model.AircraftProfile
import com.danielschatz.gliderweightbalance.data.model.StationWithPresets
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

/**
 * Datenklasse für den Simulationszustand zu einem Zeitpunkt t.
 */
data class SimulationState(
    val timeSeconds: Double,
    val totalMass: Double,
    val cgLocation: Double,
    val nonLiftingMass: Double,
    val stationMasses: Map<Int, Double>, // stationId -> aktuelle Masse (in kg)
    val maxDuration: Double
)

/**
 * Repräsentiert eine der drei markanten Schwerpunktlagen (Kopf, Abflug, Schwanz).
 */
data class ExtremeState(
    val type: Type,
    val cgLocation: Double,
    val totalMass: Double,
    val stationMasses: Map<Int, Double> // stationId -> Masse
) {
    enum class Type { FORWARD, TAKE_OFF, AFT }
}

/**
 * Interner Schnappschuss eines Zustands einer Ventilgruppe.
 */
private data class GroupSnapshot(
    val mSum: Double,
    val momSum: Double,
    val individualWeights: Map<Int, Double>
)

class SharedViewModel(application: Application) : AndroidViewModel(application) {
    private val aircraftProfileDao = AppDatabase.getDatabase(application).aircraftProfileDao()
    private val payloadStationDao: PayloadStationDao = AppDatabase.getDatabase(application).payloadStationDao()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    private val _selectedProfile = MediatorLiveData<AircraftProfile?>()
    val selectedProfile: LiveData<AircraftProfile?> = _selectedProfile

    private val _stationMasses = MutableLiveData<Map<Int, Double>>()
    
    private val _shouldAnimate = MutableLiveData<Boolean>(true)
    val shouldAnimate: LiveData<Boolean> = _shouldAnimate

    private val _totalMass = MutableLiveData<CalculationResult>()
    val totalMass: LiveData<CalculationResult> = _totalMass

    private val _cg = MutableLiveData<CalculationResult>()
    val cg: LiveData<CalculationResult> = _cg

    private val _cgRange = MutableLiveData<Pair<Double, Double>?>()
    val cgRange: LiveData<Pair<Double, Double>?> = _cgRange

    private val _nonLiftingMass = MutableLiveData<CalculationResult>()
    val nonLiftingMass: LiveData<CalculationResult > = _nonLiftingMass

    private val _isAnyStationOverloaded = MutableLiveData<Boolean>(false)
    val isAnyStationOverloaded: LiveData<Boolean> = _isAnyStationOverloaded

    private val _simulationTime = MutableLiveData<Double>(0.0)
    val simulationTime: LiveData<Double> = _simulationTime

    private val _isSimulationActive = MutableLiveData<Boolean>(false)
    val isSimulationActive: LiveData<Boolean> = _isSimulationActive

    private val _currentSimulationState = MutableLiveData<SimulationState?>()
    val currentSimulationState: LiveData<SimulationState?> = _currentSimulationState

    private val _simulationPath = MutableLiveData<List<Pair<Double, Double>>>()
    val simulationPath: LiveData<List<Pair<Double, Double>>> = _simulationPath

    private val _numOperationalGroups = MutableLiveData<Int>(0)
    val numOperationalGroups: LiveData<Int> = _numOperationalGroups

    private val _extremeStates = MutableLiveData<List<ExtremeState>?>()
    val extremeStates: LiveData<List<ExtremeState>?> = _extremeStates

    private val _headerHeight = MutableLiveData<Int>()
    val headerHeight: LiveData<Int> = _headerHeight

    fun setHeaderHeight(height: Int) {
        _headerHeight.value = height
    }

    init {
        loadInitialAircraft()
        _selectedProfile.observeForever { recalc() }
        _stationMasses.observeForever { recalc() }
        _simulationTime.observeForever { updateSimulationState() }
    }

    private fun loadInitialAircraft() {
        val lastId = prefs.getInt("last_selected_aircraft_id", R.integer.default_aircraft_id)
        if (lastId != R.integer.default_aircraft_id) {
            val source = aircraftProfileDao.getProfileById(lastId).asLiveData()
            _selectedProfile.addSource(source) { profile ->
                _selectedProfile.value = profile
                if (profile != null) {
                    val initialMasses = profile.stations.asSequence()
                        .filter { it.station.defaultValue != null }
                        .associateBy({ it.station.stationId }, { it.station.defaultValue ?: 0.0 })
                    _stationMasses.value = initialMasses
                }
                _selectedProfile.removeSource(source)
            }
        }
    }

    fun selectProfile(profile: AircraftProfile?) {
        if (profile == null) {
            _selectedProfile.value = null
            prefs.edit { putInt("last_selected_aircraft_id", -1) }
            _stationMasses.value = emptyMap()
            return
        }
        prefs.edit { putInt("last_selected_aircraft_id", profile.aircraft.id) }
        if (profile.aircraft.id > 0) {
            val source = aircraftProfileDao.getProfileById(profile.aircraft.id).asLiveData()
            _selectedProfile.addSource(source) { updatedProfile ->
                if (updatedProfile != null) {
                    _selectedProfile.value = updatedProfile
                    val initialMasses = updatedProfile.stations.asSequence()
                        .filter { it.station.defaultValue != null }
                        .associateBy({ it.station.stationId }, { it.station.defaultValue ?: 0.0 })
                    _stationMasses.value = initialMasses
                }
                _selectedProfile.removeSource(source)
            }
        } else {
            _selectedProfile.value = profile
        }
    }

    fun selectProfileById(id: Int) {
        if (id == -1 || id == R.integer.default_aircraft_id) {
            selectProfile(null)
            return
        }
        val source = aircraftProfileDao.getProfileById(id).asLiveData()
        _selectedProfile.addSource(source) { profile ->
            if (profile != null) selectProfile(profile)
            _selectedProfile.removeSource(source)
        }
    }

    fun updateStationState(stationId: Int, mass: Double, presetLabel: String?, amount: Int, isSlider: Boolean = false) {
        if (_isSimulationActive.value == true) return
        _shouldAnimate.value = !isSlider
        viewModelScope.launch(Dispatchers.IO) {
            payloadStationDao.updateStationState(stationId, mass, presetLabel, amount)
            launch(Dispatchers.Main) {
                val currentMasses = _stationMasses.value?.toMutableMap() ?: mutableMapOf()
                currentMasses[stationId] = mass
                _stationMasses.value = currentMasses
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

    private val _onScenarioApplied = MutableLiveData<Unit>()
    val onScenarioApplied: LiveData<Unit> = _onScenarioApplied

    fun applyScenarioEntries(entries: List<com.danielschatz.gliderweightbalance.data.model.ScenarioEntry>) {
        _shouldAnimate.value = true
        viewModelScope.launch(Dispatchers.IO) {
            entries.forEach { entry ->
                payloadStationDao.updateStationState(entry.stationId, entry.value ?: 0.0, entry.selectedPresetLabel, entry.amount)
            }
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
                    _selectedProfile.value = profile
                    _onScenarioApplied.value = Unit
                }
            }
        }
    }

    fun setSimulationActive(active: Boolean) {
        _isSimulationActive.value = active
        if (!active) {
            _simulationTime.value = 0.0
            recalc()
        }
    }

    fun setSimulationTime(seconds: Double) {
        _simulationTime.value = seconds
    }

    /**
     * Wählt einen der markanten Zustände für die Anzeige aus.
     */
    fun selectExtremeState(state: ExtremeState) {
        _isSimulationActive.value = true
        _currentSimulationState.value = SimulationState(
            timeSeconds = 0.0,
            totalMass = state.totalMass,
            cgLocation = state.cgLocation,
            nonLiftingMass = 0.0,
            stationMasses = state.stationMasses,
            maxDuration = 0.0
        )
        
        _totalMass.value = CalculationResult.Success(state.totalMass)
        _cg.value = CalculationResult.Success(state.cgLocation)
    }

    fun applySimulationToState() {
        val state = _currentSimulationState.value ?: return
        val profile = _selectedProfile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            profile.stations.forEach { swp ->
                if (swp.station.isConsumable) {
                    val newMass = state.stationMasses[swp.station.stationId] ?: swp.station.defaultValue ?: 0.0
                    payloadStationDao.updateStationState(swp.station.stationId, newMass, null, 1)
                }
            }
            launch(Dispatchers.Main) {
                _stationMasses.value = state.stationMasses
                profile.stations.forEach { swp ->
                    if (swp.station.isConsumable) {
                        val newMass = state.stationMasses[swp.station.stationId] ?: swp.station.defaultValue ?: 0.0
                        swp.station.defaultValue = newMass
                        swp.station.selectedPresetLabel = null
                        swp.station.amount = 1
                    }
                }
                setSimulationActive(false)
                _onScenarioApplied.value = Unit
            }
        }
    }

    private fun recalc() {
        val profile = _selectedProfile.value
        val masses = _stationMasses.value ?: emptyMap()

        if (profile == null || profile.aircraft.emptyWeight == null) {
            _totalMass.value = CalculationResult.Error
            _cg.value = CalculationResult.Error
            _nonLiftingMass.value = CalculationResult.Error
            _isAnyStationOverloaded.value = false
            _simulationPath.value = emptyList()
            return
        }

        val stationOverload = profile.stations.any { swp ->
            val rawValue = masses[swp.station.stationId] ?: 0.0
            val max = swp.station.maxMass ?: 0.0
            max > 0 && rawValue > max + 0.001
        }
        _isAnyStationOverloaded.value = stationOverload

        val payloadMass = profile.stations.sumOf { swp ->
            (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
        }
        val emptyWeight = profile.aircraft.emptyWeight!!
        val mGes = emptyWeight + payloadMass
        _totalMass.value = CalculationResult.Success(mGes)

        val emptyWeightArm = profile.aircraft.emptyWeightArm
        if (emptyWeightArm == null) {
            _cg.value = CalculationResult.Error
            _cgRange.value = null
            _simulationPath.value = emptyList()
        } else {
            val payloadMoment = profile.stations.sumOf { swp ->
                val m = (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
                m * swp.station.arm
            }
            val takeOffMoment = (emptyWeight * emptyWeightArm) + payloadMoment
            _cg.value = CalculationResult.Success(if (mGes > 0) takeOffMoment / mGes else 0.0)

            calculateBruteForceSimulation(profile, masses, emptyWeight, emptyWeightArm, mGes, takeOffMoment)
        }

        val emptyFuselage = profile.aircraft.fuselageMass ?: 0.0
        val emptyStabilizer = profile.aircraft.stabilizerMass ?: 0.0
        val payloadNonLiftingMass = profile.stations.sumOf { swp ->
            if (swp.station.isNonLifting) {
                (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
            } else 0.0
        }
        val totalNonLiftingMass = emptyFuselage + emptyStabilizer + payloadNonLiftingMass
        _nonLiftingMass.value = if (totalNonLiftingMass > 0) CalculationResult.Success(totalNonLiftingMass) else CalculationResult.Error
    }

    private fun calculateBruteForceSimulation(
        profile: AircraftProfile,
        masses: Map<Int, Double>,
        emptyWeight: Double,
        emptyWeightArm: Double,
        takeOffMass: Double,
        takeOffMoment: Double
    ) {
        val consumableStations = profile.stations.filter { it.station.isConsumable }
        if (consumableStations.isEmpty()) {
            _cgRange.value = null
            _simulationPath.value = emptyList()
            return
        }

        val coupledGroups = consumableStations.filter { it.station.couplingGroupId != 0 }
            .groupBy { it.station.couplingGroupId }.values.toList()
        val independentGroups = consumableStations.filter { it.station.couplingGroupId == 0 }
            .map { listOf(it) }
        val operationalGroups = coupledGroups + independentGroups
        
        val loadedGroupsCount = operationalGroups.count { group ->
            group.any { swp -> (masses[swp.station.stationId] ?: 0.0) > 0.1 }
        }
        _numOperationalGroups.value = loadedGroupsCount

        // 2. Erzeuge diskrete Torricelli-Schritte (inkl. aller Station-Gewichte für Rekonstruktion)
        val groupStates = operationalGroups.map { group ->
            val states = mutableListOf<GroupSnapshot>()
            val sims = group.mapNotNull { swp ->
                val curM = (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
                val maxM = (swp.station.maxMass ?: 0.0) * getDensity(swp.station.fluidType)
                val dumpT = swp.station.dumpTime ?: 0.0
                if (curM > 0 && dumpT > 0 && maxM > 0) {
                    val tStart = dumpT * (1.0 - sqrt(curM / maxM))
                    Triple(swp, tStart, dumpT)
                } else null
            }
            
            if (sims.isEmpty()) {
                val weights = group.associate { it.station.stationId to (masses[it.station.stationId] ?: 0.0) }
                states.add(GroupSnapshot(0.0, 0.0, weights))
            } else {
                val maxDuration = sims.maxOf { it.third - it.second }
                val numSteps = maxDuration.toInt().coerceIn(1, 400)
                for (i in 0..numSteps) {
                    val dt = (i.toDouble() / numSteps) * maxDuration
                    var mSum = 0.0
                    var momSum = 0.0
                    val weights = mutableMapOf<Int, Double>()
                    group.forEach { swp ->
                        val sim = sims.find { it.first.station.stationId == swp.station.stationId }
                        val mKg = if (sim != null) {
                            val t = (sim.second + dt).coerceAtMost(sim.third)
                            val maxKg = (swp.station.maxMass ?: 0.0) * getDensity(swp.station.fluidType)
                            maxKg * (1.0 - t / sim.third).pow(2.0)
                        } else 0.0
                        mSum += mKg
                        momSum += mKg * swp.station.arm
                        weights[swp.station.stationId] = mKg / getDensity(swp.station.fluidType)
                    }
                    states.add(GroupSnapshot(mSum, momSum, weights))
                }
                states.add(GroupSnapshot(0.0, 0.0, group.associate { it.station.stationId to 0.0 }))
            }
            states
        }

        var baseM = emptyWeight
        var baseMom = emptyWeight * emptyWeightArm
        val baseWeights = mutableMapOf<Int, Double>()
        profile.stations.forEach { swp ->
            if (!swp.station.isConsumable) {
                val m = (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
                baseM += m
                baseMom += m * swp.station.arm
            }
            baseWeights[swp.station.stationId] = (masses[swp.station.stationId] ?: 0.0)
        }

        // 3. Brute-Force Analyse
        var minCg = Double.MAX_VALUE
        var maxCg = Double.MIN_VALUE
        var minWeights = baseWeights.toMap()
        var maxWeights = baseWeights.toMap()

        val stepsLimit = if (operationalGroups.size >= 3) 25 else 100
        val sampledStates = groupStates.map { list ->
            if (list.size <= stepsLimit) list
            else {
                val step = list.size / stepsLimit
                list.filterIndexed { idx, _ -> idx % step == 0 || idx == list.size - 1 }
            }
        }

        fun combine(index: Int, currentM: Double, currentMom: Double, currentWeights: Map<Int, Double>) {
            if (index == sampledStates.size) {
                if (currentM > 0) {
                    val cg = currentMom / currentM
                    if (cg < minCg) {
                        minCg = cg
                        minWeights = currentWeights
                    }
                    if (cg > maxCg) {
                        maxCg = cg
                        maxWeights = currentWeights
                    }
                }
                return
            }
            for (snapshot in sampledStates[index]) {
                combine(index + 1, currentM + snapshot.mSum, currentMom + snapshot.momSum, currentWeights + snapshot.individualWeights)
            }
        }
        combine(0, baseM, baseMom, baseWeights)

        _cgRange.value = if (minCg != Double.MAX_VALUE) Pair(minCg, maxCg) else null
        
        _extremeStates.value = listOf(
            ExtremeState(ExtremeState.Type.FORWARD, minCg, minWeights.entries.sumOf { (id, weight) -> weight * getDensity(profile.stations.find { it.station.stationId == id }?.station?.fluidType) } + emptyWeight, minWeights),
            ExtremeState(ExtremeState.Type.TAKE_OFF, (takeOffMoment / takeOffMass), takeOffMass, baseWeights),
            ExtremeState(ExtremeState.Type.AFT, maxCg, maxWeights.entries.sumOf { (id, weight) -> weight * getDensity(profile.stations.find { it.station.stationId == id }?.station?.fluidType) } + emptyWeight, maxWeights)
        )

        // 5. Pfad für das Diagramm
        val path = mutableListOf<Pair<Double, Double>>()
        val simList = consumableStations.mapNotNull { swp ->
            val curM = (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
            val maxM = (swp.station.maxMass ?: 0.0) * getDensity(swp.station.fluidType)
            val dumpT = swp.station.dumpTime ?: 0.0
            if (curM > 0 && dumpT > 0 && maxM > 0) {
                val tStart = dumpT * (1.0 - sqrt(curM / maxM))
                Triple(swp, tStart, dumpT)
            } else null
        }
        if (simList.isNotEmpty()) {
            val maxT = simList.maxOf { it.third - it.second }
            for (i in 0..100) {
                val dt = (i.toDouble() / 100) * maxT
                val state = calculateSimulationAt(dt, profile, masses, simList, emptyWeight, emptyWeightArm)
                path.add(state.cgLocation to state.totalMass)
            }
        }
        _simulationPath.value = path
        updateSimulationState()
    }

    private fun updateSimulationState() {
        val profile = _selectedProfile.value ?: return
        val masses = _stationMasses.value ?: emptyMap()
        val emptyWeight = profile.aircraft.emptyWeight ?: return
        val emptyWeightArm = profile.aircraft.emptyWeightArm ?: return
        val time = _simulationTime.value ?: 0.0

        val sims = profile.stations.filter { it.station.isConsumable }.mapNotNull { swp ->
            val curM = (masses[swp.station.stationId] ?: 0.0) * getDensity(swp.station.fluidType)
            val maxM = (swp.station.maxMass ?: 0.0) * getDensity(swp.station.fluidType)
            val dumpT = swp.station.dumpTime ?: 0.0
            if (curM > 0 && dumpT > 0 && maxM > 0) {
                val tStart = dumpT * (1.0 - sqrt(curM / maxM))
                Triple(swp, tStart, dumpT)
            } else null
        }

        if (sims.isEmpty()) {
            _currentSimulationState.value = null
            return
        }

        val maxT = sims.maxOf { it.third - it.second }
        val state = calculateSimulationAt(time.coerceIn(0.0, maxT), profile, masses, sims, emptyWeight, emptyWeightArm).copy(maxDuration = maxT)
        _currentSimulationState.value = state

        if (_isSimulationActive.value == true) {
            _totalMass.value = CalculationResult.Success(state.totalMass)
            _cg.value = CalculationResult.Success(state.cgLocation)
            _nonLiftingMass.value = CalculationResult.Success(state.nonLiftingMass)
        }
    }

    private fun calculateSimulationAt(
        dt: Double,
        profile: AircraftProfile,
        takeOffMasses: Map<Int, Double>,
        sims: List<Triple<StationWithPresets, Double, Double>>,
        emptyWeight: Double,
        emptyWeightArm: Double
    ): SimulationState {
        var currentM = emptyWeight
        var currentMom = emptyWeight * emptyWeightArm
        var currentNonLiftingM = (profile.aircraft.fuselageMass ?: 0.0) + (profile.aircraft.stabilizerMass ?: 0.0)
        val currentStationMasses = mutableMapOf<Int, Double>()

        profile.stations.forEach { swp ->
            val id = swp.station.stationId
            val takeOffKg = (takeOffMasses[id] ?: 0.0) * getDensity(swp.station.fluidType)
            
            val sim = sims.find { it.first.station.stationId == id }
            val currentKg = if (sim != null) {
                val t = (sim.second + dt).coerceAtMost(sim.third)
                val maxKg = (swp.station.maxMass ?: 0.0) * getDensity(swp.station.fluidType)
                maxKg * (1.0 - t / sim.third).pow(2.0)
            } else {
                takeOffKg
            }
            
            currentM += currentKg
            currentMom += currentKg * swp.station.arm
            if (swp.station.isNonLifting) {
                currentNonLiftingM += currentKg
            }
            currentStationMasses[id] = currentKg / getDensity(swp.station.fluidType)
        }

        return SimulationState(dt, currentM, if (currentM > 0) currentMom / currentM else 0.0, currentNonLiftingM, currentStationMasses, 0.0)
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
