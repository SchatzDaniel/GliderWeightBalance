package com.example.weightbalance2.adapter

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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.weightbalance2.R
import com.example.weightbalance2.data.model.PayloadStation
import com.example.weightbalance2.data.model.Preset
import com.example.weightbalance2.databinding.ItemAddStationButtonBinding
import com.example.weightbalance2.databinding.ItemPayloadStationBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.text.isNotBlank
import kotlin.text.toDoubleOrNull
import kotlin.text.trim

interface ItemMoveCallback {
    fun onRowMoved(fromPosition: Int, toPosition: Int)
}

class PayloadStationsAdapter(
    private val onStationUpdated: (PayloadStation) -> Unit,
    private val onStationDeleted: (PayloadStation) -> Unit,
    private val onAddItem: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemMoveCallback {

    companion object {
        private const val VIEW_TYPE_STATION = 1
        private const val VIEW_TYPE_ADD_BUTTON = 2
    }

    private val stations = mutableListOf<PayloadStation>()
    var itemTouchHelper: ItemTouchHelper? = null

    // --- ViewHolder für eine normale Station ---
    inner class StationViewHolder(private val binding: ItemPayloadStationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("ClickableViewAccessibility")
        fun bind(station: PayloadStation) {
            binding.textViewStationName.text = station.name
            binding.textViewStationArm.text = itemView.context.getString(R.string.label_arm, station.arm.toString())

            // Anzeige der konfigurierten Features als kleine Icons oder Text-Labels
            val features = mutableListOf<String>()
            if (station.hasSlider) features.add("Slider")
            if (station.hasPresets) features.add("Presets")
            if (station.hasAmountInput) features.add("Menge")

            if (station.maxMass != null && station.maxMass > 0) {
                binding.textViewStationMaxMass.text = itemView.context.getString(R.string.label_max_mass, station.maxMass.toString(), station.unit)
                binding.textViewStationMaxMass.visibility = View.VISIBLE
            } else {
                binding.textViewStationMaxMass.visibility = View.GONE
            }

            binding.stationCard.setOnClickListener {
                showEditDialog(station, itemView.context)
            }

            binding.ivDragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper?.startDrag(this)
                }
                false
            }
        }

        private fun showEditDialog(station: PayloadStation, context: Context) {
            val dialogBinding = com.example.weightbalance2.databinding.DialogEditStationBinding.inflate(LayoutInflater.from(context))

            // Bestehende Felder füllen
            dialogBinding.dialogEditTextStationName.setText(station.name)
            dialogBinding.dialogEditTextStationArm.setText(station.arm.toString())
            dialogBinding.dialogEditTextStationUnit.setText(station.unit)
            dialogBinding.dialogEditTextStationMaxInput.setText(station.maxMass?.toString() ?: "")
            dialogBinding.cbIsNonLifting.isChecked = station.isNonLifting

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
            
            if (initialUnit == context.getString(R.string.unit_liter)) {
                dialogBinding.dialogStationFluidTypeLayout.visibility = View.VISIBLE
                val currentFluidLabel = fluids.entries.find { it.value == station.fluidType }?.key ?: fluidLabels[0]
                dialogBinding.dialogEditTextStationFluidType.setText(currentFluidLabel, false)
            }

            dialogBinding.dialogEditTextStationUnit.setOnItemClickListener { _, _, position, _ ->
                val selectedUnit = units[position]
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
                            hasSlider = dialogBinding.cbHasSlider.isChecked,
                            hasPresets = dialogBinding.cbHasPresets.isChecked,
                            hasAmountInput = dialogBinding.cbHasAmountInput.isChecked,
                            fluidType = newFluidType
                        ).apply() {
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
        return if (position < stations.size) VIEW_TYPE_STATION else VIEW_TYPE_ADD_BUTTON
    }

    override fun getItemCount(): Int = stations.size + 1

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
            holder.bind(stations[position])
        }
    }

    // --- Öffentliche Methoden zur Interaktion ---
    
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newStations: List<PayloadStation>) {
        stations.clear()
        stations.addAll(newStations)
        notifyDataSetChanged()
    }

    fun addStation(station: PayloadStation) {
        val insertPosition = stations.size
        stations.add(station)
        notifyItemInserted(insertPosition)
    }

    fun removeStation(station: PayloadStation) {
        val position = stations.indexOf(station)
        if (position != -1) {
            stations.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateStation(updatedStation: PayloadStation) {
        val position = if (updatedStation.stationId != 0) {
            stations.indexOfFirst { it.stationId == updatedStation.stationId }
        } else {
            stations.indexOf(updatedStation)
        }

        if (position != -1) {
            stations[position] = updatedStation
            notifyItemChanged(position)
        }
    }

    fun getCurrentStations(): List<PayloadStation> {
        return stations.toList()
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < stations.size && toPosition < stations.size) {
            val fromItem = stations.removeAt(fromPosition)
            stations.add(toPosition, fromItem)

            stations.forEachIndexed { index, station ->
                stations[index] = station.copy(displayOrder = index)
            }

            notifyItemMoved(fromPosition, toPosition)
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
            val addBinding = com.example.weightbalance2.databinding.DialogAddPresetQuickBinding.inflate(LayoutInflater.from(context))
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
