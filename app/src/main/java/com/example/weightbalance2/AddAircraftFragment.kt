package com.example.weightbalance2

import com.example.weightbalance2.adapter.PayloadStationsAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weightbalance2.data.model.Aircraft
import com.example.weightbalance2.data.model.AircraftProfile
import com.example.weightbalance2.data.model.PayloadStation
import com.example.weightbalance2.databinding.FragmentAddAircraftBinding

class AddAircraftFragment : Fragment() {
    private var _binding: FragmentAddAircraftBinding? = null
    private val binding get() = _binding!!

    // ViewModel-Instanz erstellen
    private val aircraftViewModel: AircraftViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    // Safe Args verwenden, um die übergebene ID sicher zu erhalten
    private val navArgs: AddAircraftFragmentArgs by navArgs()

    // Wird nur im Bearbeiten-Modus gesetzt. Dient als Flag und zum Halten der ID.
    private var currentAircraftProfile: AircraftProfile? = null

    private lateinit var stationsAdapter: PayloadStationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAircraftBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        val aircraftProfileId = navArgs.aircraftId

        // Appbar-Titel anpassen
        if (aircraftProfileId != (-1)) {
            activity?.title = "Flugzeug bearbeiten"
            aircraftViewModel.loadAircraftProfileById(aircraftProfileId).observe(viewLifecycleOwner) { aircraftProfile ->
                aircraftProfile?.let {
                    populateUi(it)
                }
            }
        } else {
            activity?.title = "Neues Flugzeug"
        }

        // Click Listener für den Speicher-Button einrichten
        binding.buttonSaveAircraft.setOnClickListener {
            saveOrUpdateAircraft()
        }
    }

    private fun setupRecyclerView() {
        stationsAdapter = PayloadStationsAdapter(
            onStationUpdated = { updatedStation ->
                // Die Logik zum Aktualisieren der Liste
                stationsAdapter.updateStation(updatedStation)
            },
            onStationDeleted = { stationToDelete ->
                stationsAdapter.removeStation(stationToDelete)
                Toast.makeText(context, "'${stationToDelete.name}' entfernt", Toast.LENGTH_SHORT).show()
            },
            onAddItem = {
                // Logik für das Hinzufügen einer neuen, leeren Station
                val newStation = PayloadStation(
                    // Wichtig: Generiere eine temporäre, negative ID, um sie in der Liste zu unterscheiden
                    stationId = System.currentTimeMillis().toInt() * -1,
                    aircraftOwnerId = currentAircraftProfile?.aircraft?.id ?: 0,
                    name = "Neue Station",
                    arm = 0.0,
                    unit = "kg",
                    maxMass = null,
                    displayOrder = stationsAdapter.itemCount
                )
                stationsAdapter.addStation(newStation)
            }
        )
        binding.recyclerViewStations.adapter = stationsAdapter
        binding.recyclerViewStations.layoutManager = LinearLayoutManager(requireContext())
    }


    /**
     * Füllt die UI-Felder mit den Daten eines bestehenden Flugzeugs.
     */
    private fun populateUi(profile: AircraftProfile) {
        currentAircraftProfile = profile

        // Fülle die Stammdaten
        binding.editTextRegistration.setText(profile.aircraft.registration)
        binding.editTextAircraftType.setText(profile.aircraft.aircraftType)
        binding.editTextMaxTotalMass.setText(profile.aircraft.maxTotalMass?.toString() ?: "")
        binding.editTextMaxNonLiftingMass.setText(profile.aircraft.maxNonLiftingMass?.toString() ?: "")
        binding.editTextMinCg.setText(profile.aircraft.minCg?.toString() ?: "")
        binding.editTextMaxCg.setText(profile.aircraft.maxCg?.toString() ?: "")
        binding.editTextEmptyMass.setText(profile.aircraft.emptyWeight?.toString() ?: "")
        binding.editTextFuselageMass.setText(profile.aircraft.fuselageMass?.toString() ?: "")
        binding.editTextStabilizerMass.setText(profile.aircraft.stabilizerMass?.toString() ?: "")
        binding.editTextEmptyMassArm.setText(profile.aircraft.emptyWeightArm?.toString() ?: "")

        // Übergib die Liste der Stationen an den Adapter. Die RecyclerView kümmert sich um den Rest.
        stationsAdapter.submitList(profile.stations)
    }


    /**
     * Liest die Benutzereingaben, validiert sie und speichert oder aktualisiert das Flugzeug.
     */
    /**
     * Liest die Benutzereingaben, validiert sie und speichert oder aktualisiert das Flugzeug.
     * Diese Version wurde refaktorisiert, um Code-Wiederholungen zu reduzieren.
     */

    private fun saveOrUpdateAircraft() {
        // --- Schritt 1: Lese und validiere die Pflichtfelder ---
        val registration = binding.editTextRegistration.text.toString().trim()
        val aircraftType = binding.editTextAircraftType.text.toString().trim()

        if (registration.isBlank() || aircraftType.isBlank()) {
            Toast.makeText(requireContext(), "Bitte Kennzeichen und Typ angeben.", Toast.LENGTH_LONG).show()
            return
        }

        // --- Schritt 2: Lese die Stations-Daten aus dem Adapter ---
        val stationsFromAdapter = stationsAdapter.getCurrentStations()
        val stationsForRoom = stationsFromAdapter.map { station ->
            if (station.stationId < 0) {
                // Falls die ID negativ ist, setze sie auf 0,
                // damit Room einen neuen Eintrag mit Auto-ID erstellt
                station.copy(stationId = 0)
            } else {
                station
            }
        }

        // --- Schritt 3: Erstelle die Aircraft- und Profile-Objekte ---
        val aircraft = Aircraft(
            id = currentAircraftProfile?.aircraft?.id ?: 0, // Behalte die ID im Bearbeiten-Modus
            registration = registration,
            aircraftType = aircraftType,
            maxTotalMass = binding.editTextMaxTotalMass.text.toString().toDoubleOrNull(),
            maxNonLiftingMass = binding.editTextMaxNonLiftingMass.text.toString().toDoubleOrNull(),
            minCg = binding.editTextMinCg.text.toString().toDoubleOrNull(),
            maxCg = binding.editTextMaxCg.text.toString().toDoubleOrNull(),
            emptyWeight = binding.editTextEmptyMass.text.toString().toDoubleOrNull(),
            fuselageMass = binding.editTextFuselageMass.text.toString().toDoubleOrNull(),
            stabilizerMass = binding.editTextStabilizerMass.text.toString().toDoubleOrNull(),
            emptyWeightArm = binding.editTextEmptyMassArm.text.toString().toDoubleOrNull()
        )

        // Bündle das Flugzeug und seine Stationen in einem Profil
        val profileToSave = AircraftProfile(aircraft, stationsForRoom)

        // --- Schritt 4: Speichern oder Aktualisieren über das ViewModel ---
        aircraftViewModel.saveOrUpdateProfile(profileToSave)
        sharedViewModel.selectProfile(profileToSave)

        // --- Schritt 5: Feedback und Navigation ---
        if (currentAircraftProfile == null) {
            Toast.makeText(requireContext(), "Flugzeug erfolgreich angelegt!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Flugzeug erfolgreich aktualisiert!", Toast.LENGTH_SHORT).show()
        }

        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
