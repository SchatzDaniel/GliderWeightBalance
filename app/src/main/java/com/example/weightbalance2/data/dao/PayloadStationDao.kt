package com.example.weightbalance2.data.dao

import androidx.room.*
import com.example.weightbalance2.data.model.PayloadStation
import kotlinx.coroutines.flow.Flow

@Dao
interface PayloadStationDao {

    @Query("SELECT * FROM payload_stations WHERE aircraftOwnerId = :aircraftId")
    fun getStationsForAircraft(aircraftId: Int): Flow<List<PayloadStation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stations: List<PayloadStation>)

    @Query("DELETE FROM payload_stations WHERE aircraftOwnerId = :aircraftId")
    suspend fun deleteForAircraft(aircraftId: Int)
}
