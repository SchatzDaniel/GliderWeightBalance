package com.example.weightbalance2.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class AircraftProfile(
    @Embedded
    val aircraft: Aircraft,

    @Relation(
        entity = PayloadStation::class,
        parentColumn = "id", // von Aircraft (Primärschlüssel)
        entityColumn = "aircraftOwnerId" // von PayloadStation (Fremdschlüssel)
    )
    val stations: List<StationWithPresets> // Hier nutzen wir eine neue Zwischenklasse
) {
    // Hilfsvariable für den einfachen Zugriff auf sortierte Stationen
    val sortedStations: List<PayloadStation>
        get() = stations.map { it.station }.sortedBy { it.displayOrder }
}

/**
 * Eine Hilfsklasse, die eine Station und ihre zugehörigen Presets bündelt.
 */
data class StationWithPresets(
    @Embedded val station: PayloadStation,
    @Relation(
        parentColumn = "stationId", // von PayloadStation
        entityColumn = "parentStationId" // von Preset
    )
    val presets: List<Preset>
)