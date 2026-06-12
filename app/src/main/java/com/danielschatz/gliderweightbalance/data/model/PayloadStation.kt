package com.danielschatz.gliderweightbalance.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "payload_stations")
data class PayloadStation(
    @PrimaryKey(autoGenerate = true) val stationId: Int = 0,
    val aircraftOwnerId: Int,
    val name: String,
    val arm: Double,
    val maxMass: Double? = null,
    val unit: String? = null,
    val displayOrder: Int = 0,
    val isNonLifting: Boolean = false,
    var defaultValue: Double? = null,
    val hasSlider: Boolean = false,
    val hasPresets: Boolean = false,
    val hasAmountInput: Boolean = false,
    var selectedPresetLabel: String? = null,
    var amount: Int = 1,
    val fluidType: String? = null,
    val isConsumable: Boolean = false,

    @Ignore // Dieses Feld wird nicht in der Tabelle gespeichert, nur im Code genutzt
    var presets: List<Preset> = emptyList()
) {
    // Sekundärer Konstruktor für Room, da Room @Ignore Felder nicht im Konstruktor mag
    constructor(
        stationId: Int,
        aircraftOwnerId: Int,
        name: String,
        arm: Double,
        maxMass: Double?,
        unit: String?,
        displayOrder: Int,
        isNonLifting: Boolean,
        defaultValue: Double?,
        hasSlider: Boolean,
        hasPresets: Boolean,
        hasAmountInput: Boolean,
        selectedPresetLabel: String?,
        amount: Int,
        fluidType: String?,
        isConsumable: Boolean
    ) : this(
        stationId, aircraftOwnerId, name, arm, maxMass, unit,
        displayOrder, isNonLifting, defaultValue, hasSlider,
        hasPresets, hasAmountInput, selectedPresetLabel, amount, fluidType, isConsumable, emptyList()
    )
}