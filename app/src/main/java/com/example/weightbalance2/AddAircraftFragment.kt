package com.example.weightbalance2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.weightbalance2.data.model.Aircraft
import com.example.weightbalance2.databinding.FragmentAddAircraftBinding

class AddAircraftFragment : Fragment() {
    private var _binding: FragmentAddAircraftBinding? = null
    private val binding get() = _binding!!

    // ViewModel-Instanz erstellen
    private val aircraftViewModel: AircraftViewModel by activityViewModels()

    // Safe Args verwenden, um die übergebene ID sicher zu erhalten
    private val navArgs: AddAircraftFragmentArgs by navArgs()
    private var currentAircraft: Aircraft? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAircraftBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val aircraftId = navArgs.aircraftId
        if (aircraftId != (-1)) {
            // BEARBEITEN-MODUS
            activity?.title = "Flugzeug bearbeiten" // Optional: Titel der AppBar ändern
            aircraftViewModel.loadAircraftById(aircraftId).observe(viewLifecycleOwner) { aircraft ->
                aircraft?.let {
                    populateUi(it)
                }
            }
        } else {
            activity?.title = "Neues Flugzeug" // Optional: Titel der AppBar ändern
        }

        // Click Listener für den Speicher-Button einrichten
        binding.buttonSaveAircraft.setOnClickListener {
            saveOrUpdateAircraft()
        }
    }

    /**
     * Füllt die UI-Felder mit den Daten eines bestehenden Flugzeugs.
     */
    private fun populateUi(aircraft: Aircraft) {
        currentAircraft = aircraft
        binding.editTextRegistration.setText(aircraft.registration)
        binding.editTextAircraftType.setText(aircraft.aircraftType)

        binding.maxTotalMass.setText(aircraft.maxTotalMass?.toString() ?: "")
        binding.maxNonLiftingMass.setText(aircraft.maxNonLiftingMass?.toString() ?: "")
        binding.minCg.setText(aircraft.minCg?.toString() ?: "")
        binding.maxCg.setText(aircraft.maxCg?.toString() ?: "")
        binding.emptyMass.setText(aircraft.emptyWeight?.toString() ?: "")
        binding.fuselageMass.setText(aircraft.fuselageMass?.toString() ?: "")
        binding.stabilizerMass.setText(aircraft.stabilizerMass?.toString() ?: "")
        binding.emptyArm.setText(aircraft.emptyWeightArm?.toString() ?: "")
        binding.pilotArm.setText(aircraft.pilotMassArm?.toString() ?: "")
        binding.trimBallastArm.setText(aircraft.trimBallastMassArm?.toString() ?: "")
        binding.lowerBaggageArm.setText(aircraft.lowerBaggageMassArm?.toString() ?: "")
        binding.upperBaggageArm.setText(aircraft.upperBaggageMassArm?.toString() ?: "")
        binding.waterBallastArm.setText(aircraft.waterBallastMassArm?.toString() ?: "")
        binding.stabilizerBallastArm.setText(aircraft.stabilizerBallastMassArm?.toString() ?: "")
        binding.oxygenArm.setText(aircraft.oxygenMassArm?.toString() ?: "")
        binding.instrumentArm.setText(aircraft.instrumentMassArm?.toString() ?: "")
    }


    /**
     * Liest die Benutzereingaben, validiert sie und speichert oder aktualisiert das Flugzeug.
     */
    /**
     * Liest die Benutzereingaben, validiert sie und speichert oder aktualisiert das Flugzeug.
     * Diese Version wurde refaktorisiert, um Code-Wiederholungen zu reduzieren.
     */
    // In AddAircraftFragment.kt
    private fun saveOrUpdateAircraft() {
        // --- Schritt 1: Lese und validiere die Pflichtfelder ---
        val registration = binding.editTextRegistration.text.toString().trim()
        val aircraftType = binding.editTextAircraftType.text.toString().trim()

        if (registration.isBlank() || aircraftType.isBlank()) {
            Toast.makeText(requireContext(), "Bitte Kennzeichen und Typ angeben.", Toast.LENGTH_LONG).show()
            return
        }

        // --- Schritt 2: Lese alle optionalen Felder mit toDoubleOrNull() ---
        val aircraftData = Aircraft(
            id = currentAircraft?.id ?: 0,
            registration = registration,
            aircraftType = aircraftType,

            // toDoubleOrNull() gibt bei leerem String automatisch null zurück
            maxTotalMass = binding.maxTotalMass.text.toString().toDoubleOrNull(),
            maxNonLiftingMass = binding.maxNonLiftingMass.text.toString().toDoubleOrNull(),
            minCg = binding.minCg.text.toString().toDoubleOrNull(),
            maxCg = binding.maxCg.text.toString().toDoubleOrNull(),
            emptyWeight = binding.emptyMass.text.toString().toDoubleOrNull(),
            fuselageMass = binding.fuselageMass.text.toString().toDoubleOrNull(),
            stabilizerMass = binding.stabilizerMass.text.toString().toDoubleOrNull(),
            emptyWeightArm = binding.emptyArm.text.toString().toDoubleOrNull(),
            pilotMassArm = binding.pilotArm.text.toString().toDoubleOrNull(),
            trimBallastMassArm = binding.trimBallastArm.text.toString().toDoubleOrNull(),
            lowerBaggageMassArm = binding.lowerBaggageArm.text.toString().toDoubleOrNull(),
            upperBaggageMassArm = binding.upperBaggageArm.text.toString().toDoubleOrNull(),
            waterBallastMassArm = binding.waterBallastArm.text.toString().toDoubleOrNull(),
            stabilizerBallastMassArm = binding.stabilizerBallastArm.text.toString().toDoubleOrNull(),
            oxygenMassArm = binding.oxygenArm.text.toString().toDoubleOrNull(),
            instrumentMassArm = binding.instrumentArm.text.toString().toDoubleOrNull()
        )

        // --- Schritt 3: Speichern oder Aktualisieren ---
        if (currentAircraft == null) {
            aircraftViewModel.addAircraft(aircraftData)
            Toast.makeText(requireContext(), "Flugzeug erfolgreich angelegt!", Toast.LENGTH_SHORT).show()
        } else {
            aircraftViewModel.updateAircraft(aircraftData)
            Toast.makeText(requireContext(), "Flugzeug erfolgreich aktualisiert!", Toast.LENGTH_SHORT).show()
        }
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
