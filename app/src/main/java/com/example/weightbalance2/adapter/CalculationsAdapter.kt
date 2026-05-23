package com.example.weightbalance2.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weightbalance2.data.model.StationWithPresets
import com.example.weightbalance2.databinding.ItemScrollingGroupBinding
import com.example.weightbalance2.databinding.ItemScrollingStationBinding
import java.util.Locale

class CalculationsAdapter(
    private val onWeightChanged: (Int, Double) -> Unit
) : ListAdapter<CalculationItem, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_SINGLE = 0
        private const val TYPE_GROUP = 1
    }

    fun updateData(flatList: List<StationWithPresets>) {
        val groupedList = mutableListOf<CalculationItem>()
        var currentGroup = mutableListOf<StationWithPresets>()

        for (swp in flatList) {
            val isSpecial = swp.station.hasSlider || swp.station.hasPresets
            if (isSpecial) {
                if (currentGroup.isNotEmpty()) {
                    groupedList.add(CalculationItem.StationGroup(currentGroup))
                    currentGroup = mutableListOf()
                }
                groupedList.add(CalculationItem.SingleStation(swp))
            } else {
                currentGroup.add(swp)
            }
        }
        if (currentGroup.isNotEmpty()) {
            groupedList.add(CalculationItem.StationGroup(currentGroup))
        }
        submitList(groupedList)
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is CalculationItem.SingleStation -> TYPE_SINGLE
        is CalculationItem.StationGroup -> TYPE_GROUP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SINGLE) {
            SingleViewHolder(ItemScrollingStationBinding.inflate(inflater, parent, false))
        } else {
            GroupViewHolder(ItemScrollingGroupBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is SingleViewHolder && item is CalculationItem.SingleStation) {
            holder.bind(item.swp)
        } else if (holder is GroupViewHolder && item is CalculationItem.StationGroup) {
            holder.bind(item.stations)
        }
    }

    inner class SingleViewHolder(private val binding: ItemScrollingStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(swp: StationWithPresets) {
            val station = swp.station
            binding.tvStationName.text = station.name

            // Das kombinierte Eingabe-/Anzeigefeld oben rechts
            val etWeightDisplay = binding.etManualInput

            // --- WICHTIG: Das Feld oben rechts muss IMMER sichtbar sein ---
            // Falls das übergeordnete TextInputLayout eine ID hat (z.B. layoutManualInput oben),
            // stelle sicher, dass es sichtbar ist.
            binding.layoutManualInput.visibility = View.VISIBLE
            binding.tvUnitSuffix.text = station.unit ?: "kg"

            val updateTextProgrammatically = { value: Double ->
                if (!etWeightDisplay.hasFocus()) {
                    val formatted = if (station.hasSlider) {
                        String.format(Locale.getDefault(), "%.0f", value)
                    } else {
                        String.format(Locale.getDefault(), "%.1f", value)
                    }
                    etWeightDisplay.setText(formatted)
                }
            }

            updateTextProgrammatically(station.defaultValue ?: 0.0)

            // Haupt-TextWatcher (Manuelle Eingabe oben rechts)
            etWeightDisplay.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (etWeightDisplay.hasFocus()) {
                        val weight = s.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (station.hasPresets) {
                            binding.spinnerPresets.setText("Keine Auswahl", false)
                        }
                        if (station.hasSlider && station.maxMass != null) {
                            val sliderVal = weight.toFloat().coerceIn(0f, station.maxMass.toFloat())
                            binding.weightSlider.value = sliderVal
                        }
                        onWeightChanged(station.stationId, weight)
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })

            // --- LOGIK FÜR DIE ZUSATZ-CONTROLS (UNTERHALB) ---
            if (station.hasPresets && swp.presets.isNotEmpty()) {
                binding.layoutPresetControls.visibility = View.VISIBLE
                binding.weightSlider.visibility = View.GONE

                // Mengen-Feld
                if (station.hasAmountInput) {
                    binding.layoutAmount.visibility = View.VISIBLE
                    if (binding.etAmount.text.isNullOrEmpty()) binding.etAmount.setText("1")
                } else {
                    binding.layoutAmount.visibility = View.GONE
                    binding.etAmount.setText("1")
                }

                val presetLabels = mutableListOf("Keine Auswahl")
                presetLabels.addAll(swp.presets.map { it.label })
                val adapter = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, presetLabels)
                binding.spinnerPresets.setAdapter(adapter)

                val calculateFromPresets = {
                    val selectedText = binding.spinnerPresets.text.toString()
                    val selectedPreset = swp.presets.find { it.label == selectedText }
                    if (selectedPreset != null) {
                        val amountText = binding.etAmount.text.toString()
                        val amount = if (station.hasAmountInput) amountText.toDoubleOrNull() ?: 0.0 else 1.0
                        val total = selectedPreset.weight * amount
                        updateTextProgrammatically(total)
                        onWeightChanged(station.stationId, total)
                    } else if (selectedText == "Keine Auswahl" && binding.spinnerPresets.hasFocus()) {
                        updateTextProgrammatically(0.0)
                        onWeightChanged(station.stationId, 0.0)
                    }
                }

                binding.spinnerPresets.setOnItemClickListener { _, _, _, _ -> calculateFromPresets() }
                binding.etAmount.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) { calculateFromPresets() }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                })

            } else if (station.hasSlider && station.maxMass != null && station.maxMass > 0) {
                binding.layoutPresetControls.visibility = View.GONE
                binding.weightSlider.visibility = View.VISIBLE

                binding.weightSlider.valueFrom = 0f
                binding.weightSlider.valueTo = station.maxMass.toFloat()
                binding.weightSlider.stepSize = 1.0f
                val currentVal = station.defaultValue?.toFloat() ?: 0f
                binding.weightSlider.value = currentVal.coerceIn(0f, station.maxMass.toFloat())

                binding.weightSlider.addOnChangeListener { slider, value, fromUser ->
                    if (fromUser) {
                        slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        val weight = value.toDouble()
                        updateTextProgrammatically(weight)
                        onWeightChanged(station.stationId, weight)
                    }
                }
            } else {
                // Fall: Nur manuelle Eingabe oben rechts
                binding.layoutPresetControls.visibility = View.GONE
                binding.weightSlider.visibility = View.GONE
            }
        }
    }

    inner class GroupViewHolder(private val binding: ItemScrollingGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stations: List<StationWithPresets>) {
            binding.gridLayout.removeAllViews()
            stations.forEach { swp ->
                binding.gridLayout.addView(createSmallInputView(swp))
            }
        }

        private fun createSmallInputView(swp: StationWithPresets): View {
            val context = itemView.context
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 8, 16, 8)
                // GridLayout Params korrigiert: Wir nutzen MATCH_PARENT für die Breite der Zelle
                layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                    columnSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                    width = 0
                }
            }

            val label = TextView(context).apply {
                text = swp.station.name
                textSize = 12f
            }

            val input = EditText(context).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                setText(swp.station.defaultValue?.toString() ?: "0")
                textSize = 14f
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val weight = s.toString().toDoubleOrNull() ?: 0.0
                        onWeightChanged(swp.station.stationId, weight)
                    }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                })
            }

            container.addView(label)
            container.addView(input)
            return container
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CalculationItem>() {
        override fun areItemsTheSame(oldItem: CalculationItem, newItem: CalculationItem): Boolean {
            return oldItem == newItem
        }
        override fun areContentsTheSame(oldItem: CalculationItem, newItem: CalculationItem): Boolean {
            return oldItem == newItem
        }
    }
}