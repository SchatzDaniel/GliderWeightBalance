package com.example.weightbalance2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.weightbalance2.data.database.AppDatabase
import com.example.weightbalance2.data.model.Aircraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AircraftViewModel(application: Application) : AndroidViewModel(application) {

    // The ViewModel now depends on the Repository, not the Dao directly
    private val repository: AircraftRepository

    val allAircraft: StateFlow<List<Aircraft>>

    init {
        val aircraftDao = AppDatabase.getDatabase(application).aircraftDao()
        repository = AircraftRepository(aircraftDao)
        allAircraft = repository.allAircraft.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addAircraft(aircraft: Aircraft) {
        // Launch a coroutine without specifying a dispatcher
        // The repository will handle which dispatcher to use
        viewModelScope.launch {
            repository.addAircraft(aircraft)
        }
    }

    fun loadAircraftById(id: Int): LiveData<Aircraft> {
        return repository.getAircraftById(id) // Angenommen, Sie haben ein Repository
    }

    fun updateAircraft(aircraft: Aircraft) {
        viewModelScope.launch {
            repository.updateAircraft(aircraft)
        }
    }
}