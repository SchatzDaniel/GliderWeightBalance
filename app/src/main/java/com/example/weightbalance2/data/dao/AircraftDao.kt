package com.example.weightbalance2.data.dao

import androidx.room.*
import com.example.weightbalance2.data.model.Aircraft
import kotlinx.coroutines.flow.Flow

@Dao
interface AircraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aircraft: Aircraft): Long

    @Update
    suspend fun update(aircraft: Aircraft)

    @Delete
    suspend fun delete(aircraft: Aircraft)

    @Query("SELECT * FROM aircraft_table")
    fun getAllAircraft(): Flow<List<Aircraft>>
}
