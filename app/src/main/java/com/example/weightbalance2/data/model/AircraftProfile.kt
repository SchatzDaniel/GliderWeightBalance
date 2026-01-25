package com.example.weightbalance2.data.model

import androidx.room.Embedded
import androidx.room.Relation

// Diese Klasse repräsentiert ein komplettes Flugzeug mit all seinen Teilen
data class AircraftProfile(
    @Embedded // Room versteht, dass dies Teil der Haupt-Abfrage ist
    val aircraft: Aircraft,

    @Relation(
        parentColumn = "id", // von Aircraft
        entityColumn = "aircraftOwnerId" // von PayloadStation
    )
    val stations: List<PayloadStation>
)
