package com.example.weightbalance2.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.weightbalance2.R
import com.example.weightbalance2.data.model.PayloadStation
import com.example.weightbalance2.databinding.ItemAddStationButtonBinding
import com.example.weightbalance2.databinding.ItemPayloadStationBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.text.isBlank
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
                    // Sobald das Icon berührt wird, sagen wir dem Helper: Start Drag!
                    itemTouchHelper?.startDrag(this)
                }
                false
            }
        }

        private fun showEditDialog(station: PayloadStation, context: Context) {
            val dialogBinding = com.example.weightbalance2.databinding.DialogEditStationBinding.inflate(LayoutInflater.from(context))

            dialogBinding.dialogEditTextStationName.setText(station.name)
            dialogBinding.dialogEditTextStationArm.setText(station.arm.toString())
            dialogBinding.dialogEditTextStationUnit.setText(station.unit)
            dialogBinding.dialogEditTextStationMaxInput.setText(station.maxMass?.toString() ?: "")

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

                    if (newName.isNotBlank() && newUnit.isNotBlank()) {
                        val updatedStation = station.copy(
                            name = newName,
                            arm = newArm,
                            unit = newUnit,
                            maxMass = newMaxMass
                        )
                        onStationUpdated(updatedStation)
                        dialog.dismiss()
                    } else {
                        if (newName.isBlank()) dialogBinding.dialogStationNameLayout.error = "Name wird benötigt"
                        if (newUnit.isBlank()) dialogBinding.dialogStationUnitLayout.error = "Einheit wird benötigt"
                    }
                }
                .show()
        }
    }

    // --- ViewHolder für den "Hinzufügen"-Button ---
    inner class AddButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            itemView.setOnClickListener {
                onAddItem() // Ruft den korrekten Callback auf
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
    fun submitList(newStations: List<PayloadStation>) {
        stations.clear()
        stations.addAll(newStations)
        notifyDataSetChanged() // Für die komplette Neuerstellung der Liste ist das in Ordnung.
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
        // Finde die Position der Station anhand einer eindeutigen ID.
        // Wenn die stationId noch 0 ist (neue, ungespeicherte Station), nutze Objekt-Referenz.
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
        return stations.toList() // Gib eine unveränderliche Kopie zurück
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < stations.size && toPosition < stations.size) {
            // 1. Verschiebe das Element in der Liste
            val fromItem = stations.removeAt(fromPosition)
            stations.add(toPosition, fromItem)

            // 2. WICHTIG: Aktualisiere die displayOrder für ALLE Elemente in der Liste
            // damit die neue Reihenfolge permanent im Objekt gespeichert wird
            stations.forEachIndexed { index, station ->
                stations[index] = station.copy(displayOrder = index)
            }

            // 3. Benachrichtige die UI
            notifyItemMoved(fromPosition, toPosition)
        }
    }
}
