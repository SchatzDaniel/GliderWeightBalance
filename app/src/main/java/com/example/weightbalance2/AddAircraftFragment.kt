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
        currentAircraft = aircraft // Das geladene Flugzeug für den Update-Vorgang speichern
        binding.editTextRegistration.setText(aircraft.registration)
        binding.editTextAircraftType.setText(aircraft.aircraftType)
        binding.maxTotalMass.setText(aircraft.maxTotalMass.toString())
        binding.maxNonLiftingMass.setText(aircraft.maxNonLiftingMass.toString())
        binding.minCg.setText(aircraft.minCg.toString())
        binding.maxCg.setText(aircraft.maxCg.toString())
        binding.emptyMass.setText(aircraft.emptyWeight.toString())
        binding.fuselageMass.setText(aircraft.fuselageMass.toString())
        binding.stabilizerMass.setText(aircraft.stabilizerMass.toString())
        binding.emptyArm.setText(aircraft.emptyWeightArm.toString())
        binding.pilotArm.setText(aircraft.pilotMassArm.toString())
        binding.trimBallastArm.setText(aircraft.trimBallastMassArm.toString())
        binding.lowerBaggageArm.setText(aircraft.lowerBaggageMassArm.toString())
        binding.upperBaggageArm.setText(aircraft.upperBaggageMassArm.toString())
        binding.waterBallastArm.setText(aircraft.waterBallastMassArm.toString())
        binding.stabilizerBallastArm.setText(aircraft.stabilizerBallastMassArm.toString())
        binding.oxygenArm.setText(aircraft.oxygenMassArm.toString())
        binding.instrumentArm.setText(aircraft.instrumentMassArm.toString())
    }

    /**
     * Liest die Benutzereingaben, validiert sie und speichert oder aktualisiert das Flugzeug.
     */
    /**
     * Liest die Benutzereingaben, validiert sie und speichert oder aktualisiert das Flugzeug.
     * Diese Version wurde refaktorisiert, um Code-Wiederholungen zu reduzieren.
     */
    private fun saveOrUpdateAircraft() {
        // --- Schritt 1: Definiere alle zu validierenden Felder ---
        // Wir gruppieren die UI-Felder und ihre Namen, um sie in einer Schleife zu verarbeiten.

        val fieldsToValidate = mapOf(
            "maxTotalMass" to binding.maxTotalMass,
            "maxNonLiftingMass" to binding.maxNonLiftingMass,
            "minCg" to binding.minCg,
            "maxCg" to binding.maxCg,
            "emptyWeight" to binding.emptyMass,
            "fuselageMass" to binding.fuselageMass,
            "stabilizerMass" to binding.stabilizerMass,
            "emptyWeightArm" to binding.emptyArm,
            "pilotMassArm" to binding.pilotArm,
            "trimBallastMassArm" to binding.trimBallastArm,
            "lowerBaggageMassArm" to binding.lowerBaggageArm,
            "upperBaggageMassArm" to binding.upperBaggageArm,
            "waterBallastMassArm" to binding.waterBallastArm,
            "stabilizerBallastMassArm" to binding.stabilizerBallastArm,
            "oxygenMassArm" to binding.oxygenArm,
            "instrumentMassArm" to binding.instrumentArm
        )

        val registration = binding.editTextRegistration.text.toString()
        val aircraftType = binding.editTextAircraftType.text.toString()

        // --- Schritt 2: Validierung der Text- und Zahlenfelder ---
        val validatedValues = mutableMapOf<String, Double>()
        var allFieldsValid = true

        // Prüfe Textfelder
        if (registration.isBlank() || aircraftType.isBlank()) {
            allFieldsValid = false
        }

        // Prüfe alle Zahlenfelder in einer Schleife
        for ((key, editText) in fieldsToValidate) {
            val value = editText.text.toString().toDoubleOrNull()
            if (value == null) {
                allFieldsValid = false
                break // Schleife abbrechen, wenn ein Feld ungültig ist
            }
            validatedValues[key] = value
        }

        if (!allFieldsValid) {
            Toast.makeText(requireContext(), "Bitte Kennzeichen und Typ ausfüllen.", Toast.LENGTH_LONG).show()
            return // Funktion verlassen
        }

        // --- Schritt 3: Erstelle das Aircraft-Objekt aus den validierten Daten ---
        val aircraftData = Aircraft(
            id = currentAircraft?.id ?: 0,
            registration = registration,
            aircraftType = aircraftType,
            // Die Ausrufezeichen (!!) sind jetzt sicher, weil wir wissen, dass die Werte existieren.
            maxTotalMass = validatedValues["maxTotalMass"]!!,
            maxNonLiftingMass = validatedValues["maxNonLiftingMass"]!!,
            minCg = validatedValues["minCg"]!!,
            maxCg = validatedValues["maxCg"]!!,
            emptyWeight = validatedValues["emptyWeight"]!!,
            fuselageMass = validatedValues["fuselageMass"]!!,
            stabilizerMass = validatedValues["stabilizerMass"]!!,
            emptyWeightArm = validatedValues["emptyWeightArm"]!!,
            pilotMassArm = validatedValues["pilotMassArm"]!!,
            trimBallastMassArm = validatedValues["trimBallastMassArm"]!!,
            lowerBaggageMassArm = validatedValues["lowerBaggageMassArm"]!!,
            upperBaggageMassArm = validatedValues["upperBaggageMassArm"]!!,
            waterBallastMassArm = validatedValues["waterBallastMassArm"]!!,
            stabilizerBallastMassArm = validatedValues["stabilizerBallastMassArm"]!!,
            oxygenMassArm = validatedValues["oxygenMassArm"]!!,
            instrumentMassArm = validatedValues["instrumentMassArm"]!!
        )

        // --- Schritt 4: Entscheiden, ob hinzugefügt oder aktualisiert werden soll ---
        if (currentAircraft == null) {
            aircraftViewModel.addAircraft(aircraftData)
            Toast.makeText(requireContext(), "Flugzeug erfolgreich hinzugefügt!", Toast.LENGTH_SHORT).show()
        } else {
            aircraftViewModel.updateAircraft(aircraftData)
            Toast.makeText(requireContext(), "Flugzeug erfolgreich aktualisiert!", Toast.LENGTH_SHORT).show()
        }

        // Zurück zum vorherigen Bildschirm navigieren
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
