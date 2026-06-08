package com.danielschatz.gliderweightbalance.adapter

import com.danielschatz.gliderweightbalance.data.model.StationWithPresets

sealed class CalculationItem {
    data class SingleStation(val swp: StationWithPresets) : CalculationItem()
    data class StationGroup(val stations: List<StationWithPresets>) : CalculationItem()
}