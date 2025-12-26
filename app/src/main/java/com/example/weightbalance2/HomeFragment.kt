package com.example.weightbalance2

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.Navigation

import com.example.weightbalance2.databinding.FragmentHomeBinding

class HomeFragment : Fragment(){

    lateinit var navController: NavController

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    // Variable, um die Standard-Textfarbe zu speichern
    private var defaultTextColor: ColorStateList? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        defaultTextColor = binding.twGesamtgewichtOutput.textColors

        sharedViewModel.selectedAircraft.observe(viewLifecycleOwner) { aircraft ->
            val mainActivity = activity as? MainActivity

            if (aircraft == null) {
                // Fall 1: Kein Flugzeug ausgewählt
                mainActivity?.setToolbarTitle(getString(R.string.no_aircraft_selected_title))
            } else {
                // Fall 2: Ein Flugzeug ist ausgewählt
                val title = buildString {
                    append(aircraft.registration) // Innerhalb des else-Blocks ist 'aircraft' sicher nicht null
                    append(" (")
                    append(aircraft.aircraftType)
                    append(")")
                }
                mainActivity?.setToolbarTitle(title)
            }


            sharedViewModel.recalc()
        }

        sharedViewModel.totalMass.observe(viewLifecycleOwner) { result ->
            when (result) {
                is CalculationResult.Success -> {
                    // Gültiger Wert empfangen
                    binding.twGesamtgewichtOutput.text = String.format("%.1f", result.value)

                    // Führe hier die Prüfung gegen maxTotalMass durch
                    val maxMass = sharedViewModel.maxTotalMass.value ?: 0.0
                    if (result.value > maxMass && maxMass > 0.0) {
                        // Rote Warnfarben setzen
                        binding.twGesamtgewichtOutput.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.error_text_color
                            )
                        )
                        binding.twGesamtgewichtOutput.setBackgroundResource(R.color.error_background_color)
                    } else {
                        // Standardfarben wiederherstellen
                        binding.twGesamtgewichtOutput.setTextColor(defaultTextColor)
                        binding.twGesamtgewichtOutput.setBackgroundResource(android.R.color.transparent)
                    }
                }

                is CalculationResult.Error -> {
                    // Fehlerfall
                    binding.twGesamtgewichtOutput.text =
                        getString(R.string.error_text) // z.B. "Error"
                }
            }
        }

        sharedViewModel.cg.observe(viewLifecycleOwner) { result ->
            sharedViewModel.cg.observe(viewLifecycleOwner) { result ->
                when (result) {
                    is CalculationResult.Success -> {
                        // -- 1. Schwerpunktlage (das hast du schon) --
                        binding.twSchwerpunktlageErgebnis.text = String.format("%.2f", result.value)

                        // Hole min und max CG sicher
                        val minCG = sharedViewModel.minCG.value ?: 0.0
                        val maxCG = sharedViewModel.maxCG.value ?: 0.0

                        // Färbung für die Schwerpunktlage
                        if ((minCG > 0.0 || maxCG > 0.0) && (result.value < minCG || result.value > maxCG)) {
                            binding.twSchwerpunktlageErgebnis.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.error_text_color
                                )
                            )
                            binding.twSchwerpunktlageErgebnis.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.error_background_color
                                )
                            )
                        } else {
                            binding.twSchwerpunktlageErgebnis.setTextColor(defaultTextColor)
                            binding.twSchwerpunktlageErgebnis.setBackgroundResource(android.R.color.transparent)
                        }

                        // -- 2. Prozentuale Lage (NEU & KORRIGIERT) --
                        val range = maxCG - minCG
                        if (range > 0) {
                            // Nur berechnen, wenn der Bereich gültig ist (verhindert Division durch Null)
                            val percentage = (result.value - minCG) / range * 100
                            binding.twPercentResult.text =
                                String.format("%.1f %%", percentage) // "XX.X %"
                        } else {
                            // Zeige nichts an, wenn min/maxCG nicht gesetzt sind
                            binding.twPercentResult.text = ""
                        }
                    }

                    is CalculationResult.Error -> {
                        // Fehlerfall für BEIDE Textfelder
                        binding.twSchwerpunktlageErgebnis.text = getString(R.string.error_text)
                        binding.twPercentResult.text =
                            getString(R.string.error_text) // Oder "" wenn du es leer haben willst
                    }
                }
            }

// Beobachte nonLiftingMass
            sharedViewModel.nonLiftingMass.observe(viewLifecycleOwner) { result ->
                when (result) {
                    is CalculationResult.Success -> {
                        binding.twMasseNTTeileErgebnis.text = String.format("%.1f", result.value)
                        // Prüfe gegen maxNonLiftingMass... (deine bestehende Logik)
                        val maxNonLiftingMass = sharedViewModel.maxNonLiftingMass.value ?: 0.0
                        if (result.value > maxNonLiftingMass) {
                            binding.twMasseNTTeileErgebnis.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.error_text_color)
                            )
                            binding.twMasseNTTeileErgebnis.setBackgroundColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.error_background_color
                                )
                            )
                        } else {
                            binding.twMasseNTTeileErgebnis.setTextColor(defaultTextColor)
                            binding.twMasseNTTeileErgebnis.setBackgroundResource(android.R.color.transparent)
                        }
                    }

                    is CalculationResult.Error -> {
                        binding.twMasseNTTeileErgebnis.text = getString(R.string.error_text)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}