package com.example.weightbalance2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weightbalance2.data.model.AircraftProfile
import com.example.weightbalance2.databinding.ItemAircraftCardBinding

/**
 * Adapter, der eine Liste von AircraftProfilen anzeigt.
 * Die Callbacks liefern das gesamte AircraftProfile-Objekt zurück,
 * damit der Empfänger auf alle zugehörigen Daten zugreifen kann.
 */
class AircraftAdapter(
    // Die Callbacks wurden angepasst, um AircraftProfile zu verwenden
    private val onItemClicked: (AircraftProfile) -> Unit,
    private val onEditClicked: (AircraftProfile) -> Unit,
    private val onItemLongClicked: (AircraftProfile) -> Unit
) : ListAdapter<AircraftProfile, AircraftAdapter.AircraftViewHolder>(DiffCallback) { // ViewHolder-Klasse richtig referenziert

    /**
     * Erstellt einen neuen ViewHolder für das Item-Layout.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AircraftViewHolder {
        val binding = ItemAircraftCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AircraftViewHolder(binding)
    }

    /**
     * Bindet die Daten eines AircraftProfile-Objekts an einen ViewHolder
     * und registriert die Click-Listener.
     */
    override fun onBindViewHolder(holder: AircraftViewHolder, position: Int) {
        val currentProfile = getItem(position)
        holder.bind(currentProfile)

        // Setze die Click Listener mit dem korrekten Profil-Objekt
        holder.itemView.setOnClickListener {
            onItemClicked(currentProfile)
        }
        holder.binding.btnEdit.setOnClickListener {
            onEditClicked(currentProfile)
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClicked(currentProfile)
            true // Wichtig: true zurückgeben, um den Klick als "behandelt" zu markieren
        }
    }

    /**
     * Der ViewHolder, der die UI-Elemente für eine einzelne Flugzeug-Karte hält.
     * Er ist jetzt eine "inner class", um direkten Zugriff auf Adapter-Eigenschaften zu haben, falls nötig.
     */
    inner class AircraftViewHolder(
        val binding: ItemAircraftCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Die bind-Methode erwartet jetzt ein AircraftProfile
        fun bind(profile: AircraftProfile) {
            val aircraft = profile.aircraft // Extrahiere das reine Flugzeug-Objekt für die Stammdaten

            binding.tvAircraftName.text = buildString {
                append(aircraft.registration)
                append(" (")
                append(aircraft.aircraftType)
                append(")")
            }

            // Die Detail-Anzeige kann jetzt sogar noch informativer sein
            binding.tvAircraftDetails.text = buildString {
                append("Leermasse: ${aircraft.emptyWeight ?: "N/A"} kg")
                append(" | CG: ${aircraft.emptyWeightArm ?: "N/A"} mm")
                // Zeige an, wie viele Stationen für dieses Flugzeug konfiguriert sind
                append("\nStationen: ${profile.stations.size}")
            }
        }
    }

    /**
     * Der DiffUtil.ItemCallback wurde korrigiert, um AircraftProfile-Objekte zu vergleichen.
     * Er vergleicht die Objekte anhand ihrer inneren Daten, um die Performance zu optimieren.
     */
    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<AircraftProfile>() {
            // Prüft, ob es sich um dasselbe Item handelt (z.B. gleiche ID)
            override fun areItemsTheSame(oldItem: AircraftProfile, newItem: AircraftProfile): Boolean {
                return oldItem.aircraft.id == newItem.aircraft.id
            }

            // Prüft, ob der Inhalt des Items sich geändert hat (z.B. anderer Name, andere Stationsanzahl)
            override fun areContentsTheSame(oldItem: AircraftProfile, newItem: AircraftProfile): Boolean {
                return oldItem == newItem // Data Classes haben eine korrekte equals-Implementierung
            }
        }
    }
}
