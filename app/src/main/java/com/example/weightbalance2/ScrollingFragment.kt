package com.example.weightbalance2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weightbalance2.adapter.CalculationsAdapter
import com.example.weightbalance2.databinding.FragmentScrollingBinding

class ScrollingFragment : Fragment() {

    private var _binding: FragmentScrollingBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    // Wir nutzen jetzt den neuen CalculationsAdapter
    private lateinit var calculationsAdapter: CalculationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScrollingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        // Initialisierung des neuen Adapters
        calculationsAdapter = CalculationsAdapter { stationId, newWeight ->
            // Wenn sich ein Gewicht im Adapter ändert (Preset-Wahl, Slider, etc.),
            // informieren wir das SharedViewModel für die Gesamtberechnung.
            sharedViewModel.updateStationMass(stationId, newWeight)
        }

        binding.recyclerViewMassInputs.apply {
            adapter = calculationsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        // Wir beobachten das selectedProfile (welches jetzt StationWithPresets enthält)
        sharedViewModel.selectedProfile.observe(viewLifecycleOwner) { profile ->
            if (profile == null) {
                binding.recyclerViewMassInputs.isVisible = false
                binding.textViewNoAircraftSelected.isVisible = true
                calculationsAdapter.submitList(emptyList())
            } else {
                binding.recyclerViewMassInputs.isVisible = true
                binding.textViewNoAircraftSelected.isVisible = false

                // Wir übergeben die Liste der Stationen (inkl. ihrer Presets) an den Adapter.
                // Achte darauf, dass das Profil im ViewModel bereits sortiert wurde.
                calculationsAdapter.submitList(profile.stations)
            }
        }

        // Optional: Falls du die Gesamtmasse/Schwerpunkt direkt im Fragment anzeigen willst,
        // kannst du hier weitere LiveData aus dem sharedViewModel beobachten.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}