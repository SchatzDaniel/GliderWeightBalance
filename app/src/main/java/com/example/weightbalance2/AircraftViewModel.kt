package com.example.weightbalance2

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.weightbalance2.data.database.AppDatabase
import com.example.weightbalance2.data.model.AircraftProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AircraftViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AircraftRepository
    private val prefs = application.getSharedPreferences("WeightBalancePrefs", Context.MODE_PRIVATE)

    // allProfiles kann ein StateFlow bleiben, wenn du es in anderen Flows nutzen willst
    val allProfiles: StateFlow<List<AircraftProfile>>

    init {
        val aircraftDao = AppDatabase.getDatabase(application).aircraftDao()
        val stationDao = AppDatabase.getDatabase(application).payloadStationDao()
        val profileDao = AppDatabase.getDatabase(application).aircraftProfileDao()

        repository = AircraftRepository(aircraftDao, stationDao, profileDao)

        allProfiles = repository.getAllProfiles().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * LÄDT das zuletzt ausgewählte Profil und gibt es als LiveData zurück.
     * Die Umwandlung von Flow zu LiveData findet jetzt HIER statt.
     */
    fun loadLastSelectedAircraftProfile(): LiveData<AircraftProfile?> {
        val lastId = prefs.getInt("last_selected_aircraft_id", -1)
        return repository.getProfileById(lastId).asLiveData()
    }

    /**
     * LÄDT ein Profil anhand seiner ID und gibt es als LiveData zurück.
     * Dies ist die Funktion, die dein AddAircraftFragment aufruft.
     */
    fun loadAircraftProfileById(id: Int): LiveData<AircraftProfile?> {
        return repository.getProfileById(id).asLiveData()
    }

    /**
     * SPEICHERT oder aktualisiert ein komplettes Profil.
     * Dies ist eine "fire-and-forget" Aktion, sie benötigt keinen Rückgabewert für die UI.
     */
    fun saveOrUpdateProfile(profile: AircraftProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    /**
     * LÖSCHT ein komplettes Profil.
     */
    fun deleteAircraftProfile(aircraftProfile: AircraftProfile){
        viewModelScope.launch {
            repository.deleteProfile(aircraftProfile)
        }
    }
}
