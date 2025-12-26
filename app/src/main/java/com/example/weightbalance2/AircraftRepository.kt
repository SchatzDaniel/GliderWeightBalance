package com.example.weightbalance2

import androidx.lifecycle.LiveData
import com.example.weightbalance2.data.dao.AircraftDao
import com.example.weightbalance2.data.model.Aircraft

class AircraftRepository(private val aircraftDao: AircraftDao) {

    val allAircraft = aircraftDao.getAllAircraft()

    suspend fun addAircraft(aircraft: Aircraft) {
        aircraftDao.insertAircraft(aircraft)
    }

    fun getAircraftById(id: Int): LiveData<Aircraft> {
        return aircraftDao.getAircraftById(id)
    }

    suspend fun updateAircraft(aircraft: Aircraft) {
        aircraftDao.updateAircraft(aircraft)
    }

}
