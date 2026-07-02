package com.danielschatz.gliderweightbalance

import com.danielschatz.gliderweightbalance.data.dao.AircraftDao
import com.danielschatz.gliderweightbalance.data.dao.AircraftProfileDao
import com.danielschatz.gliderweightbalance.data.dao.PayloadStationDao
import com.danielschatz.gliderweightbalance.data.dao.PresetDao
import com.danielschatz.gliderweightbalance.data.model.AircraftProfile
import kotlinx.coroutines.flow.Flow

class AircraftRepository(
    private val aircraftDao: AircraftDao,
    private val stationDao: PayloadStationDao,
    private val profileDao: AircraftProfileDao,
    private val presetDao: PresetDao
) {

    fun getAllProfiles(): Flow<List<AircraftProfile>> =
        profileDao.getAllProfiles()

    fun getProfileById(id: Int): Flow<AircraftProfile?> =
        profileDao.getProfileById(id)

    suspend fun saveProfile(profile: AircraftProfile): Int {
        val aircraft = profile.aircraft

        // 1. Aircraft speichern und ID erhalten
        val aircraftId = if (aircraft.id == 0) {
            aircraftDao.insert(aircraft).toInt()
        } else {
            aircraftDao.update(aircraft)
            aircraft.id
        }

        // 2. Bestehende Stationen aus der DB holen
        val currentStations = stationDao.getStationsForAircraftSync(aircraftId)
        val incomingStationIds = profile.stations.map { it.station.stationId }.filter { it > 0 }

        // 3. Stationen löschen, die in der neuen Liste nicht mehr enthalten sind
        currentStations.forEach { existingStation ->
            if (existingStation.stationId !in incomingStationIds) {
                stationDao.delete(existingStation)
            }
        }

        // 4. Eingehende Stationen verarbeiten (Update oder Insert)
        profile.stations.forEach { stationWithPresets ->
            val station = stationWithPresets.station
            
            val finalStationId = if (station.stationId <= 0) {
                // Neue Station einfügen
                stationDao.insert(station.copy(
                    stationId = 0,
                    aircraftOwnerId = aircraftId
                )).toInt()
            } else {
                // Bestehende Station aktualisieren
                stationDao.update(station.copy(aircraftOwnerId = aircraftId))
                station.stationId
            }

            // 5. Presets für diese Station synchronisieren
            // Da ScenarioEntry nur auf stationId linkt, können wir Presets neu anlegen
            presetDao.deleteForStation(finalStationId)
            val presetsToInsert = stationWithPresets.presets.map { preset ->
                preset.copy(
                    presetId = 0,
                    parentStationId = finalStationId
                )
            }

            if (presetsToInsert.isNotEmpty()) {
                presetDao.insertAll(presetsToInsert)
            }
        }
        return aircraftId
    }

    suspend fun deleteProfile(profile: AircraftProfile) {
        aircraftDao.delete(profile.aircraft)
        // PayloadStations werden per ON DELETE CASCADE automatisch entfernt
    }
}
