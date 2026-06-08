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

    suspend fun saveProfile(profile: AircraftProfile) {
        val aircraft = profile.aircraft

        // 1. Aircraft speichern und ID erhalten
        val aircraftId = if (aircraft.id == 0) {
            aircraftDao.insert(aircraft).toInt()
        } else {
            aircraftDao.update(aircraft)
            // Bei einem Update löschen wir die alten Stationen (und damit per Cascade die Presets),
            // um sie dann sauber neu einzufügen (einfachste Strategie für konsistente IDs)
            stationDao.deleteForAircraft(aircraft.id)
            aircraft.id
        }

        // 2. Stationen und deren Presets speichern
        profile.stations.forEach { stationWithPresets ->
            // Station für dieses Flugzeug vorbereiten (ID auf 0 für Neu-Insert)
            val stationToInsert = stationWithPresets.station.copy(
                stationId = 0,
                aircraftOwnerId = aircraftId
            )

            // Station einfügen und die neue stationId erhalten
            val newStationId = stationDao.insert(stationToInsert).toInt()

            // 3. Presets dieser Station speichern
            val presetsToInsert = stationWithPresets.presets.map { preset ->
                preset.copy(
                    presetId = 0, // Neu anlegen
                    parentStationId = newStationId // Verknüpfung zur neuen Station
                )
            }

            if (presetsToInsert.isNotEmpty()) {
                presetDao.insertAll(presetsToInsert)
            }
        }
    }

    suspend fun deleteProfile(profile: AircraftProfile) {
        aircraftDao.delete(profile.aircraft)
        // PayloadStations werden per ON DELETE CASCADE automatisch entfernt
    }
}
