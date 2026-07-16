package com.danielschatz.gliderweightbalance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "scenario_entries",
    foreignKeys = [
        ForeignKey(
            entity = Scenario::class,
            parentColumns = ["id"],
            childColumns = ["scenarioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PayloadStation::class,
            parentColumns = ["stationId"],
            childColumns = ["stationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scenarioId"), Index("stationId")]
)
data class ScenarioEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scenarioId: Int,
    val stationId: Int,
    val value: Double?,
    val selectedPresetLabel: String?,
    val amount: Int
)
