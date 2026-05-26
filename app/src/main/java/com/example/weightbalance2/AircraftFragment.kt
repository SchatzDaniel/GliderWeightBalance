package com.example.weightbalance2

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weightbalance2.data.model.AircraftProfile
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

    private var pendingExportJson: String? = null

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportToFile(it) }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importFromFile(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAircraftBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.title_activity_aircraft)
        setupRecyclerView()
        setupMenu()

        // Schritt 2: Daten beobachten und die UI (Liste und Sichtbarkeit) aktualisieren.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allProfiles.collect { aircrafts ->
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
            onItemClicked = { aircraftProfile ->
                sharedViewModel.selectProfile(aircraftProfile)
                findNavController().navigateUp()

            },
            // Klick auf Bearbeiten-Button
            onEditClicked = { aircraftProfile ->
                val action = AircraftFragmentDirections.actionAircraftFragmentToAddAircraftFragment(
                    aircraftProfile.aircraft.id)
                findNavController().navigate(action)
            },
            // Langes Drücken: Zeige den Bestätigungsdialog zum Löschen
            onItemLongClicked = { aircraftProfile ->
                showDeleteConfirmationDialog(aircraftProfile)
            },
            onExportClicked = { aircraftProfile ->
                pendingExportJson = viewModel.exportProfileToJson(aircraftProfile)
                val fileName = "${aircraftProfile.aircraft.registration.replace("/", "_")}_profile.json"
                createDocumentLauncher.launch(fileName)
            }
        )

        // Weise den Adapter und das Layout dem RecyclerView zu.
        binding.recyclerViewAircraft.apply {
            adapter = aircraftAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showDeleteConfirmationDialog(aircraftProfile: AircraftProfile) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_aircraft_title, aircraftProfile.aircraft.registration))
            .setMessage(R.string.delete_aircraft_confirmation_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteAircraftProfile(aircraftProfile)
                if (sharedViewModel.selectedProfile.value?.aircraft?.id == aircraftProfile.aircraft.id) {
                    sharedViewModel.selectProfile(null)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.aircraft_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_import -> {
                        openDocumentLauncher.launch(arrayOf("application/json"))
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun exportToFile(uri: Uri) {
        val json = pendingExportJson ?: return
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            Toast.makeText(requireContext(), R.string.export_success, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.error_export_failed, Toast.LENGTH_SHORT).show()
        } finally {
            pendingExportJson = null
        }
    }

    private fun importFromFile(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                val json = inputStream.bufferedReader().readText()
                viewModel.importProfileFromJson(json,
                    onSuccess = { Toast.makeText(requireContext(), R.string.import_success, Toast.LENGTH_SHORT).show() },
                    onError = { error -> Toast.makeText(requireContext(), "${getString(R.string.error_import_failed)}: $error", Toast.LENGTH_LONG).show() }
                )
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.error_import_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Wichtig für Speicherbereinigung
    }
}
