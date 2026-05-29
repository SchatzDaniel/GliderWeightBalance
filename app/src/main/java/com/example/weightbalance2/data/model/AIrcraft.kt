package com.example.weightbalance2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aircraft_table")
data class Aircraft(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val aircraftType: String,
    val registration: String,

    val maxTotalMass: Double?,
    val maxNonLiftingMass: Double?,
    val minCg: Double?,
    val maxCg: Double?,

    val emptyWeight: Double?,
    val fuselageMass: Double?,
    val stabilizerMass: Double?,

    val emptyWeightArm: Double?,
    val wingArea: Double?
)

