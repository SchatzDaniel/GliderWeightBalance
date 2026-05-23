package com.example.weightbalance2.adapter

import com.example.weightbalance2.data.model.StationWithPresets

sealed class CalculationItem {
    data class SingleStation(val swp: StationWithPresets) : CalculationItem()
    data class StationGroup(val stations: List<StationWithPresets>) : CalculationItem()
}