package com.example.weightbalance2.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.weightbalance2.data.model.PayloadStation
import com.example.weightbalance2.databinding.ItemMassInputBinding

// Der Callback-Typ ist eine Funktion, die (stationId, neueMasse) meldet
typealias OnMassChanged = (Int, Double) -> Unit

class MassInputAdapter(private val onMassChanged: OnMassChanged) :
    ListAdapter<PayloadStation, MassInputAdapter.MassViewHolder>(DiffCallback) {

    // Speichert die aktuell eingegebenen Massen, um die UI nicht ständig neu zu setzen
    private val currentMasses = mutableMapOf<Int, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MassViewHolder {
        val binding = ItemMassInputBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MassViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MassViewHolder, position: Int) {
        val station = getItem(position)
        holder.bind(station)
    }

    inner class MassViewHolder(private val binding: ItemMassInputBinding) : RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: TextWatcher? = null

        fun bind(station: PayloadStation) {
            // Alten Watcher entfernen, um Schleifen zu vermeiden
            binding.massInputEditText.removeTextChangedListener(textWatcher)

            binding.massInputEditText.hint = station.name
            binding.massInputEditText.setText(currentMasses[station.stationId] ?: "")

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val text = s.toString()
                    currentMasses[station.stationId] = text // Aktuelle Eingabe speichern
                    val mass = text.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onMassChanged(station.stationId, mass)
                }
            }
            binding.massInputEditText.addTextChangedListener(textWatcher)
        }
    }

    // Setzt die internen Massen zurück (wichtig bei Flugzeugwechsel)
    fun resetMasses() {
        currentMasses.clear()
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PayloadStation>() {
            override fun areItemsTheSame(oldItem: PayloadStation, newItem: PayloadStation): Boolean {
                return oldItem.stationId == newItem.stationId
            }
            override fun areContentsTheSame(oldItem: PayloadStation, newItem: PayloadStation): Boolean {
                return oldItem == newItem
            }
        }
    }
}
