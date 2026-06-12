package com.danielschatz.gliderweightbalance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.danielschatz.gliderweightbalance.adapter.CalculationsAdapter
import com.danielschatz.gliderweightbalance.databinding.FragmentScrollingBinding

class ScrollingFragment : Fragment() {

    private var _binding: FragmentScrollingBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    // Wir nutzen jetzt den neuen com.danielschatz.gliderweightbalance.adapter.CalculationsAdapter
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
        calculationsAdapter = CalculationsAdapter { stationId, newWeight, selectedPreset, amount ->
            // Wenn sich ein Gewicht im Adapter ändert (Preset-Wahl, Slider, etc.),
            // informieren wir das SharedViewModel für die Gesamtberechnung.
            sharedViewModel.updateStationState(stationId, newWeight, selectedPreset, amount)
        }

        binding.recyclerViewMassInputs.apply {
            adapter = calculationsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            
            // WICHTIG: Change-Animationen deaktivieren, um das "Warpen" der Hints zu verhindern
            (itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun observeViewModel() {
        sharedViewModel.headerHeight.observe(viewLifecycleOwner) { height ->
            binding.recyclerViewMassInputs.setPadding(
                binding.recyclerViewMassInputs.paddingLeft,
                height,
                binding.recyclerViewMassInputs.paddingRight,
                binding.recyclerViewMassInputs.paddingBottom
            )
        }

        sharedViewModel.selectedProfile.observe(viewLifecycleOwner) { profile ->
            if (profile == null) {
                binding.recyclerViewMassInputs.isVisible = false
                binding.textViewNoAircraftSelected.isVisible = true
                calculationsAdapter.submitList(emptyList())
            } else {
                binding.recyclerViewMassInputs.isVisible = true
                binding.textViewNoAircraftSelected.isVisible = false

                // Wir überlassen dem ListAdapter/DiffUtil die Arbeit.
                // notifyDataSetChanged() würde die Payloads umgehen und Flackern verursachen.
                calculationsAdapter.updateData(profile.stations)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}