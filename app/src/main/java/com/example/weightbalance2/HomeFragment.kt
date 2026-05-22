// In HomeFragment.kt

package com.example.weightbalance2

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import com.example.weightbalance2.data.model.AircraftProfile
import com.example.weightbalance2.databinding.FragmentHomeBinding
import androidx.navigation.findNavController

class HomeFragment : Fragment() {

    private lateinit var navController: NavController
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()
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
        navController = view.findNavController()
        defaultTextColor = binding.twGesamtgewichtOutput.textColors

        sharedViewModel.selectedProfile.observe(viewLifecycleOwner) { aircraftProfile ->
            updateUiForSelectedProfile(aircraftProfile)
        }

        setupCalculationObservers()
    }

    /**
     * Eine zentrale Funktion, die die UI aktualisiert, die vom Flugzeug abhängt.
     */
    private fun updateUiForSelectedProfile(aircraftProfile: AircraftProfile?) {
        if (aircraftProfile == null) {
            // Titel-Setzen hier entfernen, macht jetzt MainActivity
            childFragmentManager.beginTransaction()
                .replace(R.id.bottom_fragment_container, NoAircraftSelectedFragment())
                .commit()
        } else {
            // Titel-Setzen hier entfernen
            childFragmentManager.beginTransaction()
                .replace(R.id.bottom_fragment_container, ScrollingFragment())
                .commit()
        }
    }

    /**
     * Bündelt die restlichen Observer, um onViewCreated sauber zu halten.
     */
    private fun setupCalculationObservers() {
        sharedViewModel.totalMass.observe(viewLifecycleOwner) { result ->
            when (result) {
                is CalculationResult.Success -> {
                    // Gültiger Wert empfangen
                    binding.twGesamtgewichtOutput.text =
                        String.format(java.util.Locale.getDefault(),"%.1f", result.value)
                    updateDashboardVisuals()
                }

                is CalculationResult.Error -> {
                    binding.twGesamtgewichtOutput.text = getString(R.string.error_text)
                }
            }
        }

        sharedViewModel.cg.observe(viewLifecycleOwner) { result ->
            when (result) {
                is CalculationResult.Success -> {
                    // -- 1. Schwerpunktlage (das hast du schon) --
                    binding.twSchwerpunktlageErgebnis.text =
                        String.format(java.util.Locale.getDefault(),"%.2f", result.value)
                    updateDashboardVisuals()

                    // -- 2. Prozentuale Lage (NEU & KORRIGIERT) --
                    val minCG = sharedViewModel.selectedProfile.value?.aircraft?.minCg ?: 0.0
                    val maxCG = sharedViewModel.selectedProfile.value?.aircraft?.maxCg ?: 0.0
                    val range = maxCG - minCG
                    if (range > 0) {
                        // Nur berechnen, wenn der Bereich gültig ist (verhindert Division durch Null)
                        val percentage = (result.value - minCG) / range * 100
                        binding.twPercentResult.text =
                            String.format(java.util.Locale.getDefault(),"%.1f %%", percentage)
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

        sharedViewModel.nonLiftingMass.observe(viewLifecycleOwner) { result ->
            when (result) {
                is CalculationResult.Success -> {
                    binding.twMasseNTTeileErgebnis.text =
                        String.format(java.util.Locale.getDefault(),"%.1f", result.value)
                    updateDashboardVisuals()
                }

                is CalculationResult.Error -> {
                    binding.twMasseNTTeileErgebnis.text = getString(R.string.error_text)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun updateDashboardVisuals() {
        val profile = sharedViewModel.selectedProfile.value ?: return

        // 1. Werte aus dem ViewModel holen (CalculationResult entpacken)
        val currentTotalMass = (sharedViewModel.totalMass.value as? CalculationResult.Success)?.value ?: 0.0
        val currentCg = (sharedViewModel.cg.value as? CalculationResult.Success)?.value ?: 0.0
        val currentNonLiftingMass = (sharedViewModel.nonLiftingMass.value as? CalculationResult.Success)?.value ?: 0.0

        // 2. Grenzwerte aus dem Profil holen
        val maxMass = profile.aircraft.maxTotalMass ?: 0.0
        val minCG = profile.aircraft.minCg ?: 0.0
        val maxCG = profile.aircraft.maxCg ?: 0.0
        val maxNonLiftingMass = profile.aircraft.maxNonLiftingMass ?: 0.0

        // 3. Fehlerprüfung
        val isMassError = maxMass > 0.0 && currentTotalMass > maxMass
        val isCgError = (minCG > 0.0 || maxCG > 0.0) && (currentCg < minCG || currentCg > maxCG)
        val isNonLiftingError = maxNonLiftingMass > 0.0 && currentNonLiftingMass > maxNonLiftingMass

        val hasError = isMassError || isCgError || isNonLiftingError

        // 4. UI Aktualisierung
        if (hasError) {
            val errorColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.error_text_color2))
            val errorBg = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.error_background_light))

            binding.dashboardCard.cardElevation = 0f
            binding.dashboardCard.maxCardElevation = 0f

            binding.dashboardCard.setCardBackgroundColor(errorBg)
            binding.dashboardCard.setStrokeColor(errorColor)
            binding.dashboardCard.strokeWidth = 8

            // Optional: Einzelne Texte rot färben, wenn sie den Fehler verursachen
            binding.twGesamtgewichtOutput.setTextColor(if (isMassError) errorColor else defaultTextColor!!)
            binding.twSchwerpunktlageErgebnis.setTextColor(if (isCgError) errorColor else defaultTextColor!!)
            binding.twMasseNTTeileErgebnis.setTextColor(if (isNonLiftingError) errorColor else defaultTextColor!!)
        } else {
            val defaultElevation = 4 * resources.displayMetrics.density
            binding.dashboardCard.cardElevation = defaultElevation
            binding.dashboardCard.maxCardElevation = defaultElevation

            val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
            binding.dashboardCard.setCardBackgroundColor(ColorStateList.valueOf(surfaceColor))
            binding.dashboardCard.strokeWidth = 0

            // Farben zurücksetzen
            binding.twGesamtgewichtOutput.setTextColor(defaultTextColor!!)
            binding.twSchwerpunktlageErgebnis.setTextColor(defaultTextColor!!)
            binding.twMasseNTTeileErgebnis.setTextColor(defaultTextColor!!)
        }
    }

}
