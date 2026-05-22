package com.example.weightbalance2.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weightbalance2.data.model.StationWithPresets
import com.example.weightbalance2.databinding.ItemScrollingStationBinding

class CalculationsAdapter(
    private val onWeightChanged: (Int, Double) -> Unit // stationId, neuesGewicht
) : ListAdapter<StationWithPresets, CalculationsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScrollingStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemScrollingStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(swp: StationWithPresets) {
            val station = swp.station
            binding.tvStationName.text = station.name

            // Hilfsfunktion: Berechnet das Gewicht basierend auf Preset und Menge
            fun updatePresetWeight() {
                val selectedText = binding.spinnerPresets.text.toString()
                val preset = swp.presets.find { it.label == selectedText }
                val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 1.0

                if (preset != null) {
                    val totalWeight = preset.weight * amount
                    binding.tvStationWeight.text = String.format("%.1f %s", totalWeight, station.unit ?: "kg")
                    onWeightChanged(station.stationId, totalWeight)
                }
            }

            // 1. Logik für Presets & Menge
            if (station.hasPresets && swp.presets.isNotEmpty()) {
                binding.layoutPresetControls.visibility = View.VISIBLE
                binding.layoutManualInput.visibility = View.GONE

                val adapter = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, swp.presets.map { it.label })
                binding.spinnerPresets.setAdapter(adapter)

                // Listener für Preset-Auswahl
                binding.spinnerPresets.setOnItemClickListener { _, _, _, _ ->
                    updatePresetWeight()
                }

                // Mengen-Feld Logik
                if (station.hasAmountInput) {
                    binding.layoutAmount.visibility = View.VISIBLE
                    binding.etAmount.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) { updatePresetWeight() }
                    })
                } else {
                    binding.layoutAmount.visibility = View.GONE
                }

            } else {
                // 2. Logik für Manuelle Eingabe / Slider
                binding.layoutPresetControls.visibility = View.GONE
                binding.layoutManualInput.visibility = View.VISIBLE

                // Textfeld Listener
                binding.etWeight.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val weight = s.toString().toDoubleOrNull() ?: 0.0
                        binding.tvStationWeight.text = String.format("%.1f %s", weight, station.unit ?: "kg")
                        onWeightChanged(station.stationId, weight)

                        // Slider synchronisieren (ohne Endlosschleife)
                        if (station.hasSlider) {
                            binding.weightSlider.value = weight.coerceIn(0.0, station.maxMass ?: 1000.0).toFloat()
                        }
                    }
                })

                // Slider Logik
                if (station.hasSlider && station.maxMass != null) {
                    binding.weightSlider.visibility = View.VISIBLE
                    binding.weightSlider.valueFrom = 0f
                    binding.weightSlider.valueTo = station.maxMass.toFloat()
                    binding.weightSlider.addOnChangeListener { _, value, fromUser ->
                        if (fromUser) {
                            binding.etWeight.setText(value.toString())
                        }
                    }
                } else {
                    binding.weightSlider.visibility = View.GONE
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<StationWithPresets>() {
        override fun areItemsTheSame(oldItem: StationWithPresets, newItem: StationWithPresets) =
            oldItem.station.stationId == newItem.station.stationId
        override fun areContentsTheSame(oldItem: StationWithPresets, newItem: StationWithPresets) =
            oldItem == newItem
    }
}