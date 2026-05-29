package com.example.weightbalance2.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import com.example.weightbalance2.R
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weightbalance2.data.model.StationWithPresets
import com.example.weightbalance2.databinding.ItemScrollingGroupBinding
import com.example.weightbalance2.databinding.ItemScrollingStationBinding
import java.util.Locale
import kotlin.math.roundToInt

class CalculationsAdapter(
    private val onWeightChanged: (Int, Double, String?, Int) -> Unit
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

        private var manualWatcher: TextWatcher? = null
        private var amountWatcher: TextWatcher? = null

        fun bind(swp: StationWithPresets) {
            val station = swp.station
            binding.tvStationName.text = station.name

            // Alten Watcher entfernen
            binding.etManualInput.removeTextChangedListener(manualWatcher)
            binding.etAmount.removeTextChangedListener(amountWatcher)

            // Das kombinierte Eingabe-/Anzeigefeld oben rechts
            val etWeightDisplay = binding.etManualInput

            // --- WICHTIG: Das Feld oben rechts muss IMMER sichtbar sein ---
            // Falls das übergeordnete TextInputLayout eine ID hat (z.B. layoutManualInput oben),
            // stelle sicher, dass es sichtbar ist.
            binding.layoutManualInput.visibility = View.VISIBLE
            binding.tvUnitSuffix.text = station.unit ?: "kg"

            val updateTextProgrammatically = { value: Double ->
                if (!etWeightDisplay.hasFocus()) {
                    val formatted = if (value % 1.0 == 0.0) {
                        String.format(Locale.getDefault(), "%.0f", value)
                    } else {
                        String.format(Locale.getDefault(), "%.1f", value)
                    }
                    etWeightDisplay.setText(formatted)
                }
            }

            updateTextProgrammatically(station.defaultValue ?: 0.0)

            // Haupt-TextWatcher (Manuelle Eingabe oben rechts)
            manualWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (etWeightDisplay.hasFocus()) {
                        val weight = s.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (station.hasPresets) {
                            binding.spinnerPresets.setText(R.string.option_no_selection)
                        }
                        if (station.hasSlider && station.maxMass != null) {
                            val sliderVal = weight.toFloat().coerceIn(0f, station.maxMass.toFloat())
                            binding.weightSlider.value = sliderVal
                        }
                        onWeightChanged(station.stationId, weight, null, 1)
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            }
            etWeightDisplay.addTextChangedListener(manualWatcher)

            // --- LOGIK FÜR DIE ZUSATZ-CONTROLS (UNTERHALB) ---
            if (station.hasPresets && swp.presets.isNotEmpty()) {
                binding.layoutPresetControls.visibility = View.VISIBLE
                binding.weightSlider.visibility = View.GONE
                binding.layoutManualInput.visibility = View.VISIBLE

                // Mengen-Feld
                if (station.hasAmountInput) {
                    binding.layoutAmount.visibility = View.VISIBLE
                    binding.etAmount.setText(station.amount.toString())
                } else {
                    binding.layoutAmount.visibility = View.GONE
                    binding.etAmount.setText("1")
                }

                val presetLabels = mutableListOf("Keine Auswahl")
                presetLabels.addAll(swp.presets.map { it.label })
                val adapter = ArrayAdapter(itemView.context, android.R.layout.simple_dropdown_item_1line, presetLabels)
                binding.spinnerPresets.setAdapter(adapter)

                // Gespeicherten Zustand wiederherstellen
                binding.spinnerPresets.setText(station.selectedPresetLabel ?: "Keine Auswahl", false)

                val calculateFromPresets = {
                    val selectedText = binding.spinnerPresets.text.toString()
                    val selectedPreset = swp.presets.find { it.label == selectedText }
                    if (selectedPreset != null) {
                        val amountText = binding.etAmount.text.toString()
                        val amount = if (station.hasAmountInput) amountText.toDoubleOrNull() ?: 0.0 else 1.0
                        val total = selectedPreset.weight * amount
                        updateTextProgrammatically(total)
                        onWeightChanged(station.stationId, total, selectedText, amount.toInt())
                    } else if (selectedText == "Keine Auswahl" && binding.spinnerPresets.hasFocus()) {
                        updateTextProgrammatically(0.0)
                        onWeightChanged(station.stationId, 0.0, null, 1)
                    }
                }

                binding.spinnerPresets.setOnItemClickListener { _, _, _, _ -> calculateFromPresets() }
                
                amountWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) { 
                        if (binding.etAmount.hasFocus()) {
                            calculateFromPresets()
                        }
                    }
                    override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                }
                binding.etAmount.addTextChangedListener(amountWatcher)

            } else if (station.hasSlider && station.maxMass != null && station.maxMass > 0) {
                binding.layoutPresetControls.visibility = View.GONE
                binding.weightSlider.visibility = View.VISIBLE

                binding.weightSlider.valueFrom = 0f
                binding.weightSlider.valueTo = station.maxMass.toFloat()
                
                // Dynamische stepSize: 0.1 wenn maxMass Kommastellen hat, sonst 1.0
                val step = if (station.maxMass % 1.0 != 0.0) 0.1f else 1.0f
                binding.weightSlider.stepSize = step
                
                val currentVal = station.defaultValue?.toFloat() ?: 0f
                // Sicherstellen, dass der Wert ein Vielfaches der stepSize ist, um Crash zu vermeiden
                val snappedVal = (currentVal / step).roundToInt() * step
                binding.weightSlider.value = snappedVal.coerceIn(0f, station.maxMass.toFloat())

                binding.weightSlider.addOnChangeListener { slider, value, fromUser ->
                    if (fromUser) {
                        slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        val weight = value.toDouble()
                        updateTextProgrammatically(weight)
                        onWeightChanged(station.stationId, weight, null, 1)
                    }
                }
            } else {
                // Fall: nur manuelle Eingabe oben rechts
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
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.item_small_input_grid, binding.gridLayout, false)

            // LayoutParams für das Grid (2 Spalten, gleichmäßig verteilt)
            view.layoutParams = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                width = 0
            }

            val tvName = view.findViewById<TextView>(R.id.tvSmallStationName)
            val tilInput = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSmallManualInput)
            val etInput = view.findViewById<EditText>(R.id.etSmallManualInput)

            tvName.text = swp.station.name
            
            val unit = swp.station.unit ?: "kg"
            tilInput.suffixText = unit

            val maxMass = swp.station.maxMass ?: 0.0
            val hintText = if (maxMass > 0) context.getString(R.string.max_weight_hint, String.format(Locale.getDefault(), "%.0f", maxMass), unit) else ""
            tilInput.helperText = hintText

            val formatValue = { value: Double ->
                if (value % 1.0 == 0.0) {
                    String.format(Locale.getDefault(), "%.0f", value)
                } else {
                    String.format(Locale.getDefault(), "%.1f", value)
                }
            }

            val formattedValue = swp.station.defaultValue?.let { formatValue(it) } ?: "0"
            etInput.setText(formattedValue)

            etInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (etInput.hasFocus()) {
                        val inputStr = s.toString().replace(",", ".")
                        val weight = inputStr.toDoubleOrNull() ?: 0.0
                        
                        // Validierung
                        if (maxMass > 0 && weight > maxMass) {
                            tilInput.error = hintText
                        } else {
                            tilInput.error = null
                            tilInput.helperText = hintText
                        }

                        onWeightChanged(swp.station.stationId, weight, null, 1)
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            })

            return view
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CalculationItem>() {
        override fun areItemsTheSame(oldItem: CalculationItem, newItem: CalculationItem): Boolean {
            return when {
                oldItem is CalculationItem.SingleStation && newItem is CalculationItem.SingleStation ->
                    oldItem.swp.station.stationId == newItem.swp.station.stationId
                oldItem is CalculationItem.StationGroup && newItem is CalculationItem.StationGroup ->
                    oldItem.stations.map { it.station.stationId } == newItem.stations.map { it.station.stationId }
                else -> false
            }
        }
        override fun areContentsTheSame(oldItem: CalculationItem, newItem: CalculationItem): Boolean {
            return oldItem == newItem
        }
    }
}