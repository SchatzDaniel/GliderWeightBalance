package com.example.weightbalance2.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.weightbalance2.data.model.Preset

@Dao
interface PresetDao {
    @Insert
    suspend fun insertPreset(preset: Preset)

    @Query("SELECT * FROM station_presets WHERE parentStationId = :stationId")
    fun getPresetsForStation(stationId: Int): LiveData<List<Preset>>

    @Delete
    suspend fun deletePreset(preset: Preset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<Preset>)
}