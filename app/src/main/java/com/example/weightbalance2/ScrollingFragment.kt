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

    // Wir nutzen jetzt den neuen com.example.weightbalance2.adapter.CalculationsAdapter
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
        sharedViewModel.selectedProfile.observe(viewLifecycleOwner) { profile ->
            if (profile == null) {
                binding.recyclerViewMassInputs.isVisible = false
                binding.textViewNoAircraftSelected.isVisible = true
                // Hier ist submitList okay, um die Liste zu leeren
                calculationsAdapter.submitList(emptyList())
            } else {
                binding.recyclerViewMassInputs.isVisible = true
                binding.textViewNoAircraftSelected.isVisible = false

                // WICHTIG: Nur diese eine Zeile nutzen!
                // Sie gruppiert die Daten und aktualisiert die Anzeige.
                calculationsAdapter.updateData(profile.stations)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}