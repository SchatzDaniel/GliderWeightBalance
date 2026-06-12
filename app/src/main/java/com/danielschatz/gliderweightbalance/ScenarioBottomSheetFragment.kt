package com.danielschatz.gliderweightbalance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.danielschatz.gliderweightbalance.adapter.ScenarioAdapter
import com.danielschatz.gliderweightbalance.data.model.ScenarioEntry
import com.danielschatz.gliderweightbalance.databinding.FragmentScenarioBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ScenarioBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentScenarioBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val scenarioViewModel: ScenarioViewModel by viewModels()
    private lateinit var scenarioAdapter: ScenarioAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScenarioBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()

        sharedViewModel.selectedProfile.observe(viewLifecycleOwner) { profile ->
            scenarioViewModel.setAircraftId(profile?.aircraft?.id)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            scenarioViewModel.scenarios.collect { scenarios ->
                scenarioAdapter.submitList(scenarios)
                binding.textViewEmptyScenarios.visibility = if (scenarios.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerViewScenarios.visibility = if (scenarios.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        binding.fabSaveScenario.setOnClickListener {
            showSaveScenarioDialog()
        }
    }

    private fun setupRecyclerView() {
        scenarioAdapter = ScenarioAdapter(
            onItemClicked = { scenario ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val entries = scenarioViewModel.getEntriesForScenario(scenario.id)
                    sharedViewModel.applyScenarioEntries(entries)
                    Toast.makeText(requireContext(), R.string.import_success, Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            },
            onDeleteClicked = { scenario ->
                scenarioViewModel.deleteScenario(scenario)
            }
        )

        binding.recyclerViewScenarios.apply {
            adapter = scenarioAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        }
    }

    private fun showSaveScenarioDialog() {
        val profile = sharedViewModel.selectedProfile.value ?: return
        
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.scenario_name_hint)
        }
        val container = FrameLayout(requireContext()).apply {
            setPadding(48, 16, 48, 16)
            addView(input)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.save_scenario)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString()
                if (name.isNotBlank()) {
                    val entries = profile.stations.map { swp ->
                        ScenarioEntry(
                            scenarioId = 0,
                            stationId = swp.station.stationId,
                            value = swp.station.defaultValue,
                            selectedPresetLabel = swp.station.selectedPresetLabel,
                            amount = swp.station.amount
                        )
                    }
                    scenarioViewModel.saveScenario(name, profile.aircraft.id, entries)
                    Toast.makeText(requireContext(), R.string.save_success, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
