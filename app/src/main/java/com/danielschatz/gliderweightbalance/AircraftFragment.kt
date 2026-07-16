package com.danielschatz.gliderweightbalance

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
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
import com.danielschatz.gliderweightbalance.data.model.AircraftProfile
import com.danielschatz.gliderweightbalance.databinding.FragmentAircraftBinding
import com.danielschatz.gliderweightbalance.utils.QrHelper
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.WriterException
import kotlinx.coroutines.launch

class AircraftFragment : Fragment() {

    private var _binding: FragmentAircraftBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AircraftViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var aircraftAdapter: AircraftAdapter

    private var pendingExportJson: String? = null

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportToFile(it) }
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importFromFile(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAircraftBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = getString(R.string.title_activity_aircraft)
        setupRecyclerView()
        setupMenu()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allProfiles.collect { aircrafts ->
                    aircraftAdapter.submitList(aircrafts)
                    binding.recyclerViewAircraft.isVisible = aircrafts.isNotEmpty()
                    binding.textViewEmpty.isVisible = aircrafts.isEmpty()
                }
            }
        }

        sharedViewModel.selectedProfile.observe(viewLifecycleOwner) { profile ->
            aircraftAdapter.setSelectedAircraftId(profile?.aircraft?.id)
        }

        binding.fabAddAircraft.setOnClickListener {
            val action = MainPagerFragmentDirections.actionMainPagerFragmentToAddAircraftFragment()
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView() {
        aircraftAdapter = AircraftAdapter(
            onItemClicked = { aircraftProfile ->
                sharedViewModel.selectProfile(aircraftProfile)
                (parentFragment as? MainPagerFragment)?.switchToHome()
            },
            onEditClicked = { aircraftProfile ->
                val action = MainPagerFragmentDirections.actionMainPagerFragmentToAddAircraftFragment(aircraftProfile.aircraft.id)
                findNavController().navigate(action)
            },
            onItemLongClicked = { aircraftProfile ->
                showItemOptionsDialog(aircraftProfile)
            },
            onExportClicked = { aircraftProfile ->
                showExportOptionsDialog(aircraftProfile)
            }
        )

        binding.recyclerViewAircraft.apply {
            adapter = aircraftAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showItemOptionsDialog(aircraftProfile: AircraftProfile) {
        val options = arrayOf(
            getString(R.string.duplicate),
            getString(R.string.delete)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(aircraftProfile.aircraft.registration)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> duplicateAircraft(aircraftProfile)
                    1 -> showDeleteConfirmationDialog(aircraftProfile)
                }
            }
            .show()
    }

    private fun duplicateAircraft(profile: AircraftProfile) {
        val newRegistration = getString(R.string.duplicate_prefix, profile.aircraft.registration)
        
        // Tiefe Kopie erstellen und IDs auf 0 setzen
        val duplicatedProfile = profile.copy(
            aircraft = profile.aircraft.copy(
                id = 0,
                registration = newRegistration
            ),
            stations = profile.stations.map { swp ->
                swp.copy(
                    station = swp.station.copy(
                        stationId = 0,
                        aircraftOwnerId = 0
                    ),
                    presets = swp.presets.map { it.copy(presetId = 0, parentStationId = 0) }
                )
            }
        )
        
        viewModel.saveOrUpdateProfile(duplicatedProfile) {
            Toast.makeText(requireContext(), R.string.duplicate_success, Toast.LENGTH_SHORT).show()
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
                        showImportOptionsDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showExportOptionsDialog(profile: AircraftProfile) {
        val options = arrayOf(getString(R.string.export_json), getString(R.string.share_qr_title))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.export_aircraft)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // JSON
                        pendingExportJson = viewModel.exportProfileToJson(profile)
                        val fileName = "${profile.aircraft.registration.replace("/", "_")}_profile.json"
                        createDocumentLauncher.launch(fileName)
                    }
                    1 -> showQrCodeDialog(profile)
                }
            }
            .show()
    }

    private fun showImportOptionsDialog() {
        val options = arrayOf(getString(R.string.import_json), getString(R.string.scan_qr_title))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.import_aircraft)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openDocumentLauncher.launch(arrayOf("application/json")) // JSON
                    1 -> startQrScan()
                }
            }
            .show()
    }

    private fun showQrCodeDialog(profile: AircraftProfile) {
        try {
            val qrString = QrHelper.toQrString(profile)
            val bitmap = QrHelper.generateQrBitmap(qrString)
            val imageView = ImageView(requireContext()).apply {
                setImageBitmap(bitmap)
                setPadding(16, 16, 16, 16)
                adjustViewBounds = true
            }
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.share_qr_title))
                .setMessage(R.string.qr_export_limitation_info)
                .setView(imageView)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        } catch (_: WriterException) {
            Toast.makeText(requireContext(), R.string.error_qr_too_large, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startQrScan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
            
        val scanner = GmsBarcodeScanning.getClient(requireContext(), options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.let { qrString ->
                    val profile = QrHelper.fromQrString(qrString)
                    if (profile != null) {
                        viewModel.importProfileFromObject(profile)
                        Toast.makeText(requireContext(), R.string.import_success, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), R.string.error_import_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnCanceledListener {
                // Nur Loggen, kein Toast bei einfachem Abbruch
            }
            .addOnFailureListener { e ->
                val errorMessage = when {
                    e.message?.contains("Waiting for the barcode module") == true -> 
                        "Scanner-Modul wird noch heruntergeladen. Bitte kurz warten..."
                    else -> "Fehler: ${e.localizedMessage ?: "Unbekannter Fehler"}"
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
    }

    private fun exportToFile(uri: Uri) {
        val json = pendingExportJson ?: return
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { os -> os.write(json.toByteArray()) }
            Toast.makeText(requireContext(), R.string.export_success, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.error_export_failed, Toast.LENGTH_SHORT).show()
        } finally {
            pendingExportJson = null
        }
    }

    private fun importFromFile(uri: Uri) {
        try {
            requireContext().contentResolver.openInputStream(uri)?.use { ins ->
                val json = ins.bufferedReader().readText()
                viewModel.importProfileFromJson(json,
                    onSuccess = { Toast.makeText(requireContext(), R.string.import_success, Toast.LENGTH_SHORT).show() },
                    onError = { error -> Toast.makeText(requireContext(), "${getString(R.string.error_import_failed)}: $error", Toast.LENGTH_LONG).show() }
                )
            }
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.error_import_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
