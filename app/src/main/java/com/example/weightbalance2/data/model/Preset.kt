package com.example.weightbalance2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "station_presets",
    foreignKeys = [
        ForeignKey(
            entity = PayloadStation::class,
            parentColumns = ["stationId"],
            childColumns = ["parentStationId"],
            onDelete = ForeignKey.CASCADE // Löscht Presets, wenn Station gelöscht wird
        )
    ],
    indices = [Index("parentStationId")]
)
data class Preset(
    @PrimaryKey(autoGenerate = true) val presetId: Int = 0,
    val parentStationId: Int, // Verknüpfung zur Station
    val label: String,        // Name des Presets (z.B. "Fallschirm T10")
    val weight: Double        // Gewicht des Presets
)