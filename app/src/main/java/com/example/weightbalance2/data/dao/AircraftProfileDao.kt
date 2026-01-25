package com.example.weightbalance2.data.dao

import androidx.room.*
import com.example.weightbalance2.data.model.AircraftProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface AircraftProfileDao {

    // Alle Flugzeuge mit allen zugehörigen PayloadStations
    @Transaction
    @Query("SELECT * FROM aircraft_table")
    fun getAllProfiles(): Flow<List<AircraftProfile>>

    // Ein bestimmtes Flugzeug mit seinen Stationen
    @Transaction
    @Query("SELECT * FROM aircraft_table WHERE id = :id")
    fun getProfileById(id: Int): Flow<AircraftProfile?>

}
