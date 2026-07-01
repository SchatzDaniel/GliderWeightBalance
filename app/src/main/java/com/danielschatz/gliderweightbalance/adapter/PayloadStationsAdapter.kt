package com.danielschatz.gliderweightbalance.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.danielschatz.gliderweightbalance.R
import com.danielschatz.gliderweightbalance.data.model.PayloadStation
import com.danielschatz.gliderweightbalance.data.model.Preset
import com.danielschatz.gliderweightbalance.databinding.ItemAddStationButtonBinding
import com.danielschatz.gliderweightbalance.databinding.ItemPayloadStationBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

interface ItemMoveCallback {
    fun onRowMoved(fromPosition: Int, toPosition: Int)
}

class PayloadStationsAdapter(
    private val onStationUpdated: (PayloadStation) -> Unit,
    private val onStationDeleted: (PayloadStation) -> Unit,
    private val onAddItem: () -> Unit
) : ListAdapter<PayloadStation, RecyclerView.ViewHolder>(StationDiffCallback()), ItemMoveCallback {

    companion object {
        private const val VIEW_TYPE_STATION = 1
        private const val VIEW_TYPE_ADD_BUTTON = 2
    }

    var itemTouchHelper: ItemTouchHelper? = null

    // --- ViewHolder für eine normale Station ---
    inner class StationViewHolder(private val binding: ItemPayloadStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(station: PayloadStation) {
            val context = itemView.context
            binding.textViewStationName.text = station.name
            binding.textViewStationArm.text = context.getString(R.string.label_arm, station.arm.toString())

            // Einheit & Flüssigkeits-Info
            val features = mutableListOf<String>()
            if (station.unit == context.getString(R.string.unit_liter) && station.fluidType != null) {
                val fluidName = when (station.fluidType) {
                    "WATER" -> context.getString(R.string.fluid_water)
                    "GASOLINE" -> context.getString(R.string.fluid_gasoline)
                    "KEROSENE" -> context.getString(R.string.fluid_kerosene)
                    else -> station.fluidType
                }
                binding.textViewStationUnitInfo.text = context.getString(R.string.label_unit_fluid_info, station.unit, fluidName)
            } else {
                binding.textViewStationUnitInfo.text = context.getString(R.string.label_unit_info, station.unit ?: context.getString(R.string.unit_kg))
            }

            // Anzeige der konfigurierten Features
            if (station.hasSlider) features.add(context.getString(R.string.feature_slider))
            if (station.hasPresets) {
                val count = station.presets.size
                features.add(context.getString(R.string.feature_presets_count, count))
            }
            if (station.hasAmountInput) features.add(context.getString(R.string.feature_amount))
            if (station.isNonLifting) features.add(context.getString(R.string.feature_non_lifting))
            if (station.isConsumable) features.add(context.getString(R.string.option_is_consumable).split("(")[0].trim())

            if (features.isNotEmpty()) {
                binding.textViewStationFeatures.text = features.joinToString(" | ")
                binding.textViewStationFeatures.visibility = View.VISIBLE
            } else {
                binding.textViewStationFeatures.visibility = View.GONE
            }

            if (station.maxMass != null && station.maxMass > 0) {
                binding.textViewStationMaxMass.text = context.getString(R.string.label_max_mass, station.maxMass.toString(), station.unit)
                binding.textViewStationMaxMass.visibility = View.VISIBLE
            } else {
                binding.textViewStationMaxMass.visibility = View.GONE
            }

            binding.stationCard.setOnClickListener {
                showEditDialog(station, itemView.context)
            }

            binding.ivDragHandle.setOnTouchListener { _, event: MotionEvent ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper?.startDrag(this)
                }
                false
            }
        }

        private fun showEditDialog(station: PayloadStation, context: Context) {
            val dialogBinding = com.danielschatz.gliderweightbalance.databinding.DialogEditStationBinding.inflate(LayoutInflater.from(context))

            // Bestehende Felder füllen
            dialogBinding.dialogEditTextStationName.setText(station.name)
            dialogBinding.dialogEditTextStationArm.setText(station.arm.toString())
            dialogBinding.dialogEditTextStationUnit.setText(station.unit)
            dialogBinding.dialogEditTextStationMaxInput.setText(station.maxMass?.toString() ?: "")
            dialogBinding.cbIsNonLifting.isChecked = station.isNonLifting
            dialogBinding.cbIsConsumable.isChecked = station.isConsumable

            // NEU: Feature-Checkboxen füllen
            dialogBinding.cbHasSlider.isChecked = station.hasSlider
            dialogBinding.cbHasPresets.isChecked = station.hasPresets
            dialogBinding.cbHasAmountInput.isChecked = station.hasAmountInput

            // NEU: Einheit- und Flüssigkeitstyp-Dropdowns
            val units = listOf(context.getString(R.string.unit_kg), context.getString(R.string.unit_liter))
            val unitAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, units)
            dialogBinding.dialogEditTextStationUnit.setAdapter(unitAdapter)
            
            val fluids = mapOf(
                context.getString(R.string.fluid_water) to "WATER",
                context.getString(R.string.fluid_gasoline) to "GASOLINE",
                context.getString(R.string.fluid_kerosene) to "KEROSENE"
            )
            val fluidLabels = fluids.keys.toList()
            val fluidAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, fluidLabels)
            dialogBinding.dialogEditTextStationFluidType.setAdapter(fluidAdapter)

            // Initialwerte setzen
            val initialUnit = station.unit ?: context.getString(R.string.unit_kg)
            dialogBinding.dialogEditTextStationUnit.setText(initialUnit, false)
            dialogBinding.dialogStationMaxInputLayout.suffixText = initialUnit
            
            if (initialUnit == context.getString(R.string.unit_liter)) {
                dialogBinding.dialogStationFluidTypeLayout.visibility = View.VISIBLE
                val currentFluidLabel = fluids.entries.find { it.value == station.fluidType }?.key ?: fluidLabels[0]
                dialogBinding.dialogEditTextStationFluidType.setText(currentFluidLabel, false)
            }

            dialogBinding.dialogEditTextStationUnit.setOnItemClickListener { _, _, position, _ ->
                val selectedUnit = units[position]
                dialogBinding.dialogStationMaxInputLayout.suffixText = selectedUnit
                if (selectedUnit == context.getString(R.string.unit_liter)) {
                    dialogBinding.dialogStationFluidTypeLayout.visibility = View.VISIBLE
                    if (dialogBinding.dialogEditTextStationFluidType.text.isNullOrEmpty()) {
                        dialogBinding.dialogEditTextStationFluidType.setText(fluidLabels[0], false)
                    }
                } else {
                    dialogBinding.dialogStationFluidTypeLayout.visibility = View.GONE
                }
            }

            dialogBinding.btnFluidInfo.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.fluid_info_title)
                    .setMessage(R.string.fluid_info_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }

            dialogBinding.btnConsumableInfo.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.consumable_info_title)
                    .setMessage(R.string.consumable_info_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }

            // Logik: Slider nur erlauben, wenn MaxMass gesetzt ist
            dialogBinding.cbHasSlider.isEnabled = station.maxMass != null && station.maxMass > 0
            
            dialogBinding.dialogEditTextStationMaxInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val maxVal = s.toString().toDoubleOrNull()
                    val isValid = maxVal != null && maxVal > 0
                    dialogBinding.cbHasSlider.isEnabled = isValid
                    if (!isValid) dialogBinding.cbHasSlider.isChecked = false
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            dialogBinding.btnEditPresets.setOnClickListener {
                showPresetsListDialog(context, station) { updatedPresets ->
                    station.presets = updatedPresets
                    Toast.makeText(context, context.getString(R.string.presets_count_toast, updatedPresets.size.toString()), Toast.LENGTH_SHORT).show()
                }
            }

            dialogBinding.cbHasPresets.setOnCheckedChangeListener { _, isChecked ->
                dialogBinding.btnEditPresets.visibility = if (isChecked) View.VISIBLE else View.GONE
                dialogBinding.cbHasAmountInput.isEnabled = isChecked
                if (!isChecked) {
                    dialogBinding.cbHasAmountInput.isChecked = false
                }
                if (isChecked && station.presets == null) {
                    station.presets = emptyList()
                }
            }

            dialogBinding.cbHasAmountInput.isEnabled = station.hasPresets

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.edit_station)
                .setView(dialogBinding.root)
                .setNegativeButton(R.string.delete) { dialog, _ ->
                    onStationDeleted(station)
                    dialog.dismiss()
                }
                .setPositiveButton(R.string.save) { dialog, _ ->
                    val newName = dialogBinding.dialogEditTextStationName.text.toString().trim()
                    val newArm = dialogBinding.dialogEditTextStationArm.text.toString().toDoubleOrNull() ?: station.arm
                    val newUnit = dialogBinding.dialogEditTextStationUnit.text.toString().trim()
                    val newMaxMass = dialogBinding.dialogEditTextStationMaxInput.text.toString().toDoubleOrNull()
                    
                    val selectedFluidLabel = dialogBinding.dialogEditTextStationFluidType.text.toString()
                    val newFluidType = if (newUnit == context.getString(R.string.unit_liter)) {
                        fluids[selectedFluidLabel]
                    } else null

                    if (newName.isNotBlank() && newUnit.isNotBlank()) {
                        val updatedStation = station.copy(
                            name = newName,
                            arm = newArm,
                            unit = newUnit,
                            maxMass = newMaxMass,
                            isNonLifting = dialogBinding.cbIsNonLifting.isChecked,
                            isConsumable = dialogBinding.cbIsConsumable.isChecked,
                            hasSlider = dialogBinding.cbHasSlider.isChecked,
                            hasPresets = dialogBinding.cbHasPresets.isChecked,
                            hasAmountInput = dialogBinding.cbHasAmountInput.isChecked,
                            fluidType = newFluidType
                        ).apply {
                            this.presets = station.presets
                        }
                        onStationUpdated(updatedStation)
                        dialog.dismiss()
                    }
                }
                .show()
        }
    }

    // --- ViewHolder für den "Hinzufügen"-Button ---
    inner class AddButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            itemView.setOnClickListener {
                onAddItem()
            }
        }
    }

    // --- Adapter-Methoden für mehrere View-Typen ---
    override fun getItemViewType(position: Int): Int {
        return if (position < currentList.size) VIEW_TYPE_STATION else VIEW_TYPE_ADD_BUTTON
    }

    override fun getItemCount(): Int = currentList.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_STATION -> {
                val binding = ItemPayloadStationBinding.inflate(inflater, parent, false)
                StationViewHolder(binding)
            }
            VIEW_TYPE_ADD_BUTTON -> {
                val binding = ItemAddStationButtonBinding.inflate(inflater, parent, false)
                AddButtonViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is StationViewHolder) {
            holder.bind(getItem(position))
        }
    }

    fun addStation(station: PayloadStation) {
        val newList = currentList.toMutableList()
        newList.add(station)
        submitList(newList)
    }

    fun removeStation(station: PayloadStation) {
        val newList = currentList.toMutableList()
        newList.remove(station)
        submitList(newList)
    }

    fun updateStation(updatedStation: PayloadStation) {
        val newList = currentList.toMutableList()
        val index = if (updatedStation.stationId != 0) {
            newList.indexOfFirst { it.stationId == updatedStation.stationId }
        } else {
            newList.indexOf(updatedStation)
        }

        if (index != -1) {
            newList[index] = updatedStation
            submitList(newList)
        }
    }

    fun getCurrentStations(): List<PayloadStation> {
        return currentList.toList()
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < currentList.size && toPosition < currentList.size) {
            val newList = currentList.toMutableList()
            val fromItem = newList.removeAt(fromPosition)
            newList.add(toPosition, fromItem)

            val reorderedList = newList.mapIndexed { index, station ->
                station.copy(displayOrder = index)
            }

            submitList(reorderedList)
        }
    }

    class StationDiffCallback : DiffUtil.ItemCallback<PayloadStation>() {
        override fun areItemsTheSame(oldItem: PayloadStation, newItem: PayloadStation): Boolean {
            return oldItem.stationId == newItem.stationId
        }

        override fun areContentsTheSame(oldItem: PayloadStation, newItem: PayloadStation): Boolean {
            return oldItem == newItem
        }
    }

    private fun showPresetsListDialog(
        context: Context,
        station: PayloadStation,
        onPresetsChanged: (List<Preset>) -> Unit
    ) {
        val currentPresets = station.presets.toMutableList()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_preset_manager, null)
        val listContainer = dialogView.findViewById<LinearLayout>(R.id.presetListContainer)
        val btnAdd = dialogView.findViewById<View>(R.id.btnAddPreset)

        fun refreshList() {
            listContainer.removeAllViews()
            currentPresets.forEach { preset ->
                val itemView = LayoutInflater.from(context).inflate(R.layout.item_preset_edit, listContainer, false)
                val tvInfo = itemView.findViewById<android.widget.TextView>(R.id.tvPresetInfo)
                val btnDel = itemView.findViewById<View>(R.id.btnDeletePreset)

                tvInfo.text = context.getString(R.string.preset_info_format, preset.label, preset.weight.toString())
                btnDel.setOnClickListener {
                    currentPresets.remove(preset)
                    refreshList()
                }
                listContainer.addView(itemView)
            }
        }

        btnAdd.setOnClickListener {
            val addBinding = com.danielschatz.gliderweightbalance.databinding.DialogAddPresetQuickBinding.inflate(LayoutInflater.from(context))
            MaterialAlertDialogBuilder(context)
                .setTitle("Preset hinzufügen")
                .setView(addBinding.root)
                .setPositiveButton("Hinzufügen") { _, _ ->
                    val name = addBinding.etPresetName.text.toString()
                    val weight = addBinding.etPresetWeight.text.toString().toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        currentPresets.add(Preset(parentStationId = station.stationId, label = name, weight = weight))
                        refreshList()
                    }
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }

        refreshList()

        MaterialAlertDialogBuilder(context)
            .setTitle("Presets für ${station.name}")
            .setView(dialogView)
            .setPositiveButton("Fertig") { _, _ ->
                onPresetsChanged(currentPresets)
            }
            .show()
    }

}
