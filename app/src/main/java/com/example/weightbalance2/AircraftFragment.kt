package com.example.weightbalance2

import android.os.Bundle
import android.view.LayoutInflater // Wichtig: LayoutInflater importieren
import android.view.View
import android.view.ViewGroup // Wichtig: ViewGroup importieren
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weightbalance2.data.model.Aircraft
import com.example.weightbalance2.databinding.FragmentAircraftBinding
import kotlinx.coroutines.launch

class AircraftFragment : Fragment() {

    // 1. View Binding deklarieren (korrekte Methode für Fragments)
    private var _binding: FragmentAircraftBinding? = null
    private val binding get() = _binding!!

    // 2. ViewModel-Instanzen holen
    private val viewModel: AircraftViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    // 3. Adapter-Instanz deklarieren
    private lateinit var aircraftAdapter: AircraftAdapter

    // onCreateView hinzufügen, um das Binding korrekt zu initialisieren
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAircraftBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Schritt 1: Adapter initialisieren und dem RecyclerView zuweisen.
        // Das passiert jetzt nur einmal.
        setupRecyclerView()

        // Schritt 2: Daten beobachten und die UI (Liste und Sichtbarkeit) aktualisieren.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allAircraft.collect { aircrafts ->
                    // a) Die neue Liste an den Adapter übergeben.
                    aircraftAdapter.submitList(aircrafts)

                    // b) Die Sichtbarkeit der Views basierend auf der Liste steuern.
                    // Dieser Block wird JEDES MAL ausgeführt, wenn sich die Liste ändert.
                    binding.recyclerViewAircraft.isVisible = aircrafts.isNotEmpty()
                    binding.textViewEmpty.isVisible = aircrafts.isEmpty()
                }
            }
        }

        // Schritt 3: Click Listener für den "Hinzufügen"-Button
        binding.fabAddAircraft.setOnClickListener {
            val action = AircraftFragmentDirections.actionAircraftFragmentToAddAircraftFragment()
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView() {
        aircraftAdapter = AircraftAdapter(
            // Normaler Klick: Flugzeug auswählen und zum Home-Screen navigieren
            onItemClicked = { aircraft ->
                sharedViewModel.selectAircraft(aircraft)
                findNavController().navigateUp()
            },
            // Klick auf Bearbeiten-Button
            onEditClicked = { aircraft ->
                val action = AircraftFragmentDirections.actionAircraftFragmentToAddAircraftFragment(
                    aircraft.id)
                findNavController().navigate(action)
            },
            // Langes Drücken: Zeige den Bestätigungsdialog zum Löschen
            onItemLongClicked = { aircraft ->
                showDeleteConfirmationDialog(aircraft)
            }
        )

        // Weise den Adapter und das Layout dem RecyclerView zu.
        binding.recyclerViewAircraft.apply {
            adapter = aircraftAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showDeleteConfirmationDialog(aircraft: Aircraft) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_aircraft_title, aircraft.registration))
            .setMessage(R.string.delete_aircraft_confirmation_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteAircraft(aircraft)
                if (sharedViewModel.selectedAircraft.value?.id == aircraft.id) {
                    sharedViewModel.selectAircraft(null)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Wichtig für Speicherbereinigung
    }
}
