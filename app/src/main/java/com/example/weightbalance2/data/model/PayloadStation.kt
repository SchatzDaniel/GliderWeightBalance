package com.example.weightbalance2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Neue Klasse für eine Station (Hebelarm)
@Entity(tableName = "payload_stations")
data class PayloadStation(
    @PrimaryKey(autoGenerate = true) val stationId: Int = 0,
    val aircraftOwnerId: Int, // Fremdschlüssel zum Flugzeug
    val name: String,         // z.B. "Copilot", "Gepäck"
    val arm: Double,          // Der Hebelarm in mm
    val maxMass: Double? = null, // Optional: max. Masse für diese Station
    val unit: String? = null,
    val displayOrder: Int = 0, // Für die Reihenfolge der Stationen
    val isNonLifting: Boolean = false, // Für die Berechnung des Non-Lifting Mass
    val defaultValue: Double? = null // Speichert das "Standardgewicht" für diese Station
)
