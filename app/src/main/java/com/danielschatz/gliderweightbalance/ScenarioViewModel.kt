package com.danielschatz.gliderweightbalance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danielschatz.gliderweightbalance.data.database.AppDatabase
import com.danielschatz.gliderweightbalance.data.model.Scenario
import com.danielschatz.gliderweightbalance.data.model.ScenarioEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioViewModel(application: Application) : AndroidViewModel(application) {
    private val scenarioDao = AppDatabase.getDatabase(application).scenarioDao()

    private val _currentAircraftId = MutableStateFlow<Int?>(null)
    
    val scenarios: Flow<List<Scenario>> = _currentAircraftId.flatMapLatest { id ->
        if (id != null) {
            scenarioDao.getScenariosForAircraft(id)
        } else {
            MutableStateFlow(emptyList())
        }
    }

    fun setAircraftId(id: Int?) {
        _currentAircraftId.value = id
    }

    fun saveScenario(name: String, aircraftId: Int, entries: List<ScenarioEntry>) {
        viewModelScope.launch {
            scenarioDao.saveScenarioWithEntries(Scenario(aircraftId = aircraftId, name = name), entries)
        }
    }

    fun deleteScenario(scenario: Scenario) {
        viewModelScope.launch {
            scenarioDao.deleteScenario(scenario)
        }
    }
    
    suspend fun getEntriesForScenario(scenarioId: Int): List<ScenarioEntry> {
        return scenarioDao.getEntriesForScenario(scenarioId)
    }
}
