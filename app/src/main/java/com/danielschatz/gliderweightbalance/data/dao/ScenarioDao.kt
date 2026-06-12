package com.danielschatz.gliderweightbalance.data.dao

import androidx.room.*
import com.danielschatz.gliderweightbalance.data.model.Scenario
import com.danielschatz.gliderweightbalance.data.model.ScenarioEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenario(scenario: Scenario): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenarioEntries(entries: List<ScenarioEntry>)

    @Query("SELECT * FROM scenarios WHERE aircraftId = :aircraftId")
    fun getScenariosForAircraft(aircraftId: Int): Flow<List<Scenario>>

    @Query("SELECT * FROM scenario_entries WHERE scenarioId = :scenarioId")
    suspend fun getEntriesForScenario(scenarioId: Int): List<ScenarioEntry>

    @Delete
    suspend fun deleteScenario(scenario: Scenario)

    @Transaction
    suspend fun saveScenarioWithEntries(scenario: Scenario, entries: List<ScenarioEntry>) {
        val id = insertScenario(scenario)
        val entriesWithId = entries.map { it.copy(scenarioId = id.toInt()) }
        insertScenarioEntries(entriesWithId)
    }
}
