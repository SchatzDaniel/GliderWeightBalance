package com.example.weightbalance2

import com.example.weightbalance2.data.dao.AircraftDao
import com.example.weightbalance2.data.dao.AircraftProfileDao
import com.example.weightbalance2.data.dao.PayloadStationDao
import com.example.weightbalance2.data.model.AircraftProfile
import kotlinx.coroutines.flow.Flow

class AircraftRepository(
    private val aircraftDao: AircraftDao,
    private val stationDao: PayloadStationDao,
    private val profileDao: AircraftProfileDao
) {

    fun getAllProfiles(): Flow<List<AircraftProfile>> =
        profileDao.getAllProfiles()

    fun getProfileById(id: Int): Flow<AircraftProfile?> =
        profileDao.getProfileById(id)

    suspend fun saveProfile(profile: AircraftProfile) {
        val aircraft = profile.aircraft

        if (aircraft.id == 0) {
            val newId = aircraftDao.insert(aircraft)
            val stationsWithId = profile.sortedStations.map {
                it.copy(aircraftOwnerId = newId.toInt())
            }
            stationDao.insertAll(stationsWithId)
        } else {
            aircraftDao.update(aircraft)
            stationDao.deleteForAircraft(aircraft.id)
            stationDao.insertAll(profile.sortedStations)
        }
    }

    suspend fun deleteProfile(profile: AircraftProfile) {
        aircraftDao.delete(profile.aircraft)
        // PayloadStations werden per ON DELETE CASCADE automatisch entfernt
    }
}
