package com.danielschatz.gliderweightbalance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.danielschatz.gliderweightbalance.data.database.AppDatabase
import com.danielschatz.gliderweightbalance.data.model.AircraftProfile
import com.google.gson.Gson
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AircraftViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AircraftRepository

    // allProfiles kann ein StateFlow bleiben, wenn du es in anderen Flows nutzen willst
    val allProfiles: StateFlow<List<AircraftProfile>>

    init {
        val aircraftDao = AppDatabase.getDatabase(application).aircraftDao()
        val stationDao = AppDatabase.getDatabase(application).payloadStationDao()
        val profileDao = AppDatabase.getDatabase(application).aircraftProfileDao()
        val presetDao = AppDatabase.getDatabase(application).presetDao()

        repository = AircraftRepository(aircraftDao, stationDao, profileDao, presetDao)

        allProfiles = repository.getAllProfiles().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    /**
     * LÄDT ein Profil anhand seiner ID und gibt es als LiveData zurück.
     * Dies ist die Funktion, die dein AddAircraftFragment aufruft.
     */
    fun loadAircraftProfileById(id: Int): LiveData<AircraftProfile?> {
        return repository.getProfileById(id).asLiveData().map { profile ->
            // Wir sortieren die Liste der StationWithPresets anhand der displayOrder der eingebetteten station
            profile?.copy(
                stations = profile.stations.sortedBy { it.station.displayOrder }
            )
        }
    }

    /**
     * SPEICHERT oder aktualisiert ein komplettes Profil.
     */
    fun saveOrUpdateProfile(profile: AircraftProfile, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val id = repository.saveProfile(profile)
            onComplete(id)
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

    /**
     * Konvertiert ein Profil in einen JSON-String.
     */
    fun exportProfileToJson(profile: AircraftProfile): String {
        return Gson().toJson(profile)
    }

    /**
     * Importiert ein Profil aus einem JSON-String.
     */
    fun importProfileFromJson(json: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val profile = Gson().fromJson(json, AircraftProfile::class.java)
            importProfileFromObject(profile)
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Unbekannter Fehler beim Import")
        }
    }

    fun importProfileFromObject(profile: AircraftProfile) {
        viewModelScope.launch {
            // IDs zurücksetzen, damit es als neues Flugzeug angelegt wird
            val cleanProfile = profile.copy(
                aircraft = profile.aircraft.copy(id = 0),
                stations = profile.stations.map { swp ->
                    swp.copy(
                        station = swp.station.copy(stationId = 0, aircraftOwnerId = 0),
                        presets = swp.presets.map { it.copy(presetId = 0, parentStationId = 0) }
                    )
                }
            )
            repository.saveProfile(cleanProfile)
        }
    }
}
