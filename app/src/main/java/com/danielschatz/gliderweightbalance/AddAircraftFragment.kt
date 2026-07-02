package com.danielschatz.gliderweightbalance

import com.danielschatz.gliderweightbalance.adapter.PayloadStationsAdapter
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.danielschatz.gliderweightbalance.adapter.DragManageAdapter
import com.danielschatz.gliderweightbalance.data.model.Aircraft
import com.danielschatz.gliderweightbalance.data.model.AircraftProfile
import com.danielschatz.gliderweightbalance.data.model.PayloadStation
import com.danielschatz.gliderweightbalance.databinding.FragmentAddAircraftBinding
import com.danielschatz.gliderweightbalance.data.model.StationWithPresets

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
    private var focusChangeListener: android.view.ViewTreeObserver.OnGlobalFocusChangeListener? = null

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
        if (aircraftProfileId != R.integer.default_aircraft_id) {
            activity?.title = getString(R.string.edit_aircraft)
            aircraftViewModel.loadAircraftProfileById(aircraftProfileId).observe(viewLifecycleOwner) { aircraftProfile ->
                aircraftProfile?.let {
                    populateUi(it)
                }
            }
        } else {
            activity?.title = getString(R.string.new_aircraft)
        }

        // Click Listener für den Speicher-Button einrichten
        binding.buttonSaveAircraft.setOnClickListener {
            saveOrUpdateAircraft()
        }

        setupKeyboardHandling()
    }

    private fun setupKeyboardHandling() {
        // Fokus-Listener für "Weiter"-Taste und manuelle Klicks
        focusChangeListener = android.view.ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null && newFocus is android.widget.EditText) {
                scrollToFocusedView(newFocus)
            }
        }
        binding.root.viewTreeObserver.addOnGlobalFocusChangeListener(focusChangeListener)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            // Falls die Tastatur eingeblendet wird, zum aktuellen Feld scrollen
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                _binding?.let { it.root.findFocus()?.let { view -> scrollToFocusedView(view) } }
            }

            insets
        }
    }

    private fun scrollToFocusedView(view: View) {
        val binding = _binding ?: return
        binding.addAircraftScrollView.post {
            val currentBinding = _binding ?: return@post
            val rect = android.graphics.Rect()
            view.getDrawingRect(rect)
            currentBinding.addAircraftScrollView.offsetDescendantRectToMyCoords(view, rect)

            val scrollY = currentBinding.addAircraftScrollView.scrollY
            val scrollViewHeight = currentBinding.addAircraftScrollView.height
            
            // Der Platz, den der FAB einnimmt (entspricht dem Padding im XML)
            val fabSpace = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                160f, 
                resources.displayMetrics
            ).toInt()

            // Das Feld soll im Bereich zwischen Top und dem FAB sichtbar sein
            val visibleBottom = scrollY + scrollViewHeight - fabSpace
            val visibleTop = scrollY + 100 // Kleiner Puffer nach oben

            if (rect.bottom > visibleBottom) {
                // Zu weit unten -> Scrolle es knapp über den FAB
                val targetScrollY = rect.bottom - (scrollViewHeight - fabSpace) + 24
                currentBinding.addAircraftScrollView.smoothScrollTo(0, targetScrollY)
            } else if (rect.top < visibleTop) {
                // Zu weit oben -> Scrolle es ein Stück nach unten
                currentBinding.addAircraftScrollView.smoothScrollTo(0, (rect.top - 100).coerceAtLeast(0))
            }
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
                    displayOrder = stationsAdapter.itemCount,
                    isNonLifting = false,
                    isConsumable = false
                )
                stationsAdapter.addStation(newStation)
            }
        )
        binding.recyclerViewStations.apply {
            adapter = stationsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        // ItemTouchHelper initialisieren und an die RecyclerView heften
        val callback = DragManageAdapter(stationsAdapter)
        val touchHelper = ItemTouchHelper(callback)
        stationsAdapter.itemTouchHelper = touchHelper
        touchHelper.attachToRecyclerView(binding.recyclerViewStations)
    }

    /**
     * Füllt die UI-Felder mit den Daten eines bestehenden Flugzeugs.
     */
    private fun populateUi(profile: AircraftProfile) {
        currentAircraftProfile = profile

        // --- NEU: Mapping der Stationen inkl. Presets für den Adapter ---
        val uiStations = profile.stations.map { swp ->
            // Wir nehmen das PayloadStation Objekt und weisen ihm seine Presets zu
            swp.station.apply {
                this.presets = swp.presets
            }
        }.sortedBy { it.displayOrder }

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
        binding.editTextWingArea.setText(profile.aircraft.wingArea?.toString() ?: "")

        // Übergib die gemappte Liste an den Adapter
        stationsAdapter.submitList(uiStations)
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
            val finalId = if (station.stationId < 0) 0 else station.stationId
            station.copy(stationId = finalId).apply {
                this.presets = station.presets // Presets für das ViewModel "mitschleppen"8
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
            emptyWeightArm = binding.editTextEmptyMassArm.text.toString().toDoubleOrNull(),
            wingArea = binding.editTextWingArea.text.toString().toDoubleOrNull()
        )

        // Bündle das Flugzeug und seine Stationen (inkl. deren Presets) in einem Profil
        val profileToSave = AircraftProfile(
            aircraft = aircraft,
            stations = stationsForRoom.map { station ->
                // Hier verpacken wir jede Station wieder in die Relations-Klasse
                StationWithPresets(station = station, presets = station.presets)
            }
        )

        // --- Schritt 4: Speichern oder Aktualisieren über das ViewModel ---
        aircraftViewModel.saveOrUpdateProfile(profileToSave) { newId ->
            sharedViewModel.selectProfileById(newId)

            // --- Schritt 5: Feedback und Navigation ---
            activity?.runOnUiThread {
                if (currentAircraftProfile == null) {
                    Toast.makeText(requireContext(), "Flugzeug erfolgreich angelegt!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Flugzeug erfolgreich aktualisiert!", Toast.LENGTH_SHORT).show()
                }
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.root?.viewTreeObserver?.removeOnGlobalFocusChangeListener(focusChangeListener)
        _binding = null
    }
}
