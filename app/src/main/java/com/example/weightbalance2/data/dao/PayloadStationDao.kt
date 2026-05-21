package com.example.weightbalance2.data.dao

import androidx.room.*
import com.example.weightbalance2.data.model.PayloadStation
import kotlinx.coroutines.flow.Flow

@Dao
interface PayloadStationDao {

    @Query("SELECT * FROM payload_stations WHERE aircraftOwnerId = :aircraftId ORDER BY displayOrder ASC")
    fun getStationsForAircraft(aircraftId: Int): Flow<List<PayloadStation>>

    @Query("SELECT * FROM payload_stations WHERE stationId = :stationId LIMIT 1")
    suspend fun getStationById(stationId: Int): PayloadStation?

    @Update
    suspend fun update(station: PayloadStation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stations: List<PayloadStation>)

    @Query("DELETE FROM payload_stations WHERE aircraftOwnerId = :aircraftId")
    suspend fun deleteForAircraft(aircraftId: Int)
}
