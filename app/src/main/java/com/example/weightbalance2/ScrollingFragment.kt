package com.example.weightbalance2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.weightbalance2.adapter.MassInputAdapter
import com.example.weightbalance2.databinding.FragmentScrollingBinding

class ScrollingFragment : Fragment() {

    private var _binding: FragmentScrollingBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var massInputAdapter: MassInputAdapter

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
        massInputAdapter = MassInputAdapter { stationId, newMass ->
            // Dies wird jedes Mal aufgerufen, wenn der Benutzer etwas eingibt.
            // Leite die Änderung direkt an das ViewModel weiter.
            sharedViewModel.updateStationMass(stationId, newMass)
        }
        binding.recyclerViewMassInputs.adapter = massInputAdapter
    }

    private fun observeViewModel() {
        // Beobachte das ausgewählte Flugzeugprofil.
        sharedViewModel.selectedProfile.observe(viewLifecycleOwner) { profile ->
            if (profile == null) {
                // Kein Flugzeug ausgewählt: Zeige Platzhalter und leere die Liste.
                binding.recyclerViewMassInputs.isVisible = false
                binding.textViewNoAircraftSelected.isVisible = true
                massInputAdapter.submitList(emptyList())
            } else {
                // Ein Flugzeug ist da: Zeige die RecyclerView und übergebe die Stationsliste.
                binding.recyclerViewMassInputs.isVisible = true
                binding.textViewNoAircraftSelected.isVisible = false

                // WICHTIG: Setze die internen Eingaben im Adapter zurück, bevor eine neue Liste kommt.
                massInputAdapter.resetMasses()
                massInputAdapter.submitList(profile.sortedStations)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
