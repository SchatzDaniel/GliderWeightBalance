package com.example.weightbalance2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weightbalance2.data.model.Aircraft
import com.example.weightbalance2.databinding.ItemAircraftCardBinding

class AircraftAdapter(
    private val onItemClicked: (Aircraft) -> Unit,  // Für den Klick auf das ganze Element
    private val onEditClicked: (Aircraft) -> Unit   // Für den Klick auf den Bearbeiten-Button
) : ListAdapter<Aircraft, AircraftAdapter.AircraftViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AircraftViewHolder {
        val binding = ItemAircraftCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AircraftViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AircraftViewHolder, position: Int) {
        val current = getItem(position)

        // ... Binden Sie Ihre anderen Daten (z.B. Kennzeichen) ...
        holder.binding.tvAircraftName.text = buildString {
            append(current.registration)
            append(" (")
            append(current.aircraftType)
            append(")")
        }

        // Setze die beiden Click Listener
        holder.itemView.setOnClickListener {
            onItemClicked(current)
        }
        holder.binding.btnEdit.setOnClickListener {
            onEditClicked(current)
        }
    }

    class AircraftViewHolder(
        val binding: ItemAircraftCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(aircraft: Aircraft) {
            binding.tvAircraftName.text = buildString {
                append(aircraft.registration)
                append(" (")
                append(aircraft.aircraftType)
                append(")")
            }
            binding.tvAircraftDetails.text = buildString {
                append("Leermasse: ")
                append(aircraft.emptyWeight)
                append(" kg\nCG: ")
                append(aircraft.emptyWeightArm)
                append(" mm")
            }
        }
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Aircraft>() {
            override fun areItemsTheSame(a: Aircraft, b: Aircraft) = a.id == b.id
            override fun areContentsTheSame(a: Aircraft, b: Aircraft) = a == b
        }
    }
}
