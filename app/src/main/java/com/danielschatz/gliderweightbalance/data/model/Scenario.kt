package com.danielschatz.gliderweightbalance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "scenarios",
    foreignKeys = [
        ForeignKey(
            entity = Aircraft::class,
            parentColumns = ["id"],
            childColumns = ["aircraftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("aircraftId")]
)
data class Scenario(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val aircraftId: Int,
    val name: String
)
