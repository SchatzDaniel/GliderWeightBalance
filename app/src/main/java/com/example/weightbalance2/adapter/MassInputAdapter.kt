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

class MassInputAdapter(
    private val onMassChanged: (Int, Double) -> Unit,
    private val onMassPersist: (Int, Double) -> Unit
):  ListAdapter<PayloadStation, MassInputAdapter.MassViewHolder>(DiffCallback) {

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
            // 1. Alten Watcher entfernen, um Endlosschleifen beim setText zu vermeiden
            binding.massInputEditText.removeTextChangedListener(textWatcher)

            // 2. UI-Labels und Einheiten setzen
            binding.massTextInputLayout.hint = station.name
            binding.massTextInputLayout.suffixText = station.unit
            binding.massInputEditText.hint = null
            if (station.maxMass != null && station.maxMass > 0) {
                binding.massTextInputLayout.helperText = "maximal: ${station.maxMass} ${station.unit}"
            } else {
                binding.massTextInputLayout.helperText = null
            }

            // 3. Den anzuzeigenden Text bestimmen
            // Priorität:
            // a) Aktuelle Eingabe im Cache (während der Nutzer tippt oder scrollt)
            // b) defaultValue aus der Datenbank (falls vorhanden und ungleich 0.0)
            // c) Leerer String (damit keine 0.0 erscheint)

            val dbValueFormatted = if (station.defaultValue == null || station.defaultValue == 0.0) {
                ""
            } else {
                // Verhindert .0 bei ganzen Zahlen für schönere Optik
                if (station.defaultValue % 1.0 == 0.0) station.defaultValue.toInt().toString()
                else station.defaultValue.toString()
            }

            val textToSet = currentMasses[station.stationId] ?: dbValueFormatted
            binding.massInputEditText.setText(textToSet)

            // 4. Neuen TextWatcher registrieren
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val text = s.toString()

                    // Cache aktualisieren, damit beim Scrollen der Text erhalten bleibt
                    currentMasses[station.stationId] = text

                    // Umwandlung für die Logik (Komma zu Punkt)
                    val mass = text.replace(',', '.').toDoubleOrNull() ?: 0.0

                    // Wenn Beladung über Limit, dann Helpertext Rot
                    updateHelperColor(station, mass)

                    // A) Live-Update für das Dashboard (SharedViewModel)
                    onMassChanged(station.stationId, mass)

                    // B) Persistenz-Update für die Datenbank (SharedViewModel -> DAO)
                    // Hier wird der Wert dauerhaft gespeichert
                    onMassPersist(station.stationId, mass)
                }
            }
            binding.massInputEditText.addTextChangedListener(textWatcher)
            // Am Ende der bind-Funktion in MassInputAdapter.kt:
            val currentMass = textToSet.replace(',', '.').toDoubleOrNull() ?: 0.0
            updateHelperColor(station, currentMass) // Hilfsfunktion oder den Farb-Block hier aufrufen
        }

        fun updateHelperColor(station: PayloadStation, currentMass: Double) {
            if (station.maxMass != null && currentMass > station.maxMass) {
                // Rot, wenn über Limit
                binding.massTextInputLayout.setHelperTextColor(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
                )
            } else {
                // Standardfarbe (Grau), wenn alles okay oder kein Limit
                binding.massTextInputLayout.setHelperTextColor(
                    android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(com.example.weightbalance2.R.color.helper_text_default)
                    )
                )
            }
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
