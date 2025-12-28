package com.example.weightbalance2.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.weightbalance2.data.model.Aircraft
import kotlinx.coroutines.flow.Flow

//Das DAO ist eine Schnittstelle, die Methoden für den Datenbankzugriff (Einfügen, Abfragen, Löschen
// usw.) definiert. Room generiert die Implementierung automatisch.
@Dao
interface AircraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAircraft(aircraft: Aircraft)

    @Query("SELECT * FROM aircraft_table ORDER BY registration ASC")
    fun getAllAircraft(): Flow<List<Aircraft>>

    @Query("SELECT * FROM aircraft_table WHERE id = :id")
    fun getAircraftById(id: Int): LiveData<Aircraft>

    @Update
    suspend fun updateAircraft(aircraft: Aircraft)

    @Delete
    suspend fun deleteAircraft(aircraft: Aircraft)
}
