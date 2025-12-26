package com.example.weightbalance2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weightbalance2.databinding.FragmentAircraftBinding
import kotlinx.coroutines.launch

class AircraftFragment : Fragment() {

    // 1. View Binding deklarieren
    private var _binding: FragmentAircraftBinding? = null
    private val binding get() = _binding!!

    // 2. ViewModel-Instanz holen
    private val viewModel: AircraftViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by navGraphViewModels(R.id.main_nav)

    // 3. Adapter-Instanz deklarieren
    private lateinit var aircraftAdapter: AircraftAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Layout mit View Binding inflaten
        _binding = FragmentAircraftBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialisiere den Adapter mit ZWEI verschiedenen Klick-Aktionen.
        aircraftAdapter = AircraftAdapter(
            onItemClicked = { aircraft ->
                // AKTION 1: Flugzeug als aktiv markieren
                // 1. Setze das Flugzeug im SharedViewModel
                sharedViewModel.selectAircraft(aircraft)

                // 2. Gib dem Nutzer eine visuelle Bestätigung.
                Toast.makeText(
                    requireContext(),
                    "Flugzeug '${aircraft.registration}' als aktiv ausgewählt.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onEditClicked = { aircraft ->
                // AKTION 2: Auf "Bearbeiten" klicken und zum Bearbeiten-Screen navigieren
                val action = AircraftFragmentDirections.actionAircraftFragmentToAddAircraftFragment(aircraft.id)
                findNavController().navigate(action)
            }
        )

        // 2. RecyclerView einrichten
        binding.recyclerViewAircraft.adapter = aircraftAdapter
        binding.recyclerViewAircraft.layoutManager = LinearLayoutManager(requireContext())

        // 3. ViewModel beobachten, um die Liste zu aktualisieren
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allAircraft.collect { aircrafts ->
                    aircraftAdapter.submitList(aircrafts)

                    // Sichtbarkeit der "Liste leer"-Ansicht steuern
                    binding.recyclerViewAircraft.isVisible = aircrafts.isNotEmpty()
                    binding.textViewEmpty.isVisible = aircrafts.isEmpty()
                }
            }
        }

        // 4. Click Listener für den "Hinzufügen"-Button
        binding.fabAddAircraft.setOnClickListener {
            // Navigiere zum Hinzufügen-Fragment (ohne eine ID zu übergeben)
            val action = AircraftFragmentDirections.actionAircraftFragmentToAddAircraftFragment()
            findNavController().navigate(action)
        }
    }

    private fun observeAircraftList() {
        // Starte eine Coroutine, die an den Lebenszyklus des Fragments gebunden ist
        viewLifecycleOwner.lifecycleScope.launch {
            // Dieser Block wird ausgeführt, wenn das Fragment mindestens im RESUMED-Zustand ist
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // Sammle die Daten aus dem Flow des ViewModels
                viewModel.allAircraft.collect { aircraftList ->
                    // 7. Daten an den Adapter übergeben
                    aircraftAdapter.submitList(aircraftList)

                    // 8. Sichtbarkeit der "Liste leer"-Ansicht steuern
                    binding.recyclerViewAircraft.isVisible = aircraftList.isNotEmpty()
                    binding.textViewEmpty.isVisible = aircraftList.isEmpty()
                }
            }
        }
    }

    // 9. Speicherbereinigung, um Memory Leaks zu verhindern
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
