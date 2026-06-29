package com.danielschatz.gliderweightbalance

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.danielschatz.gliderweightbalance.data.model.AircraftProfile
import com.danielschatz.gliderweightbalance.databinding.FragmentHomeBinding
import java.util.Locale

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

        // Wir nehmen die Farbe eines Standard-Labels als Referenz
        defaultTextColor = binding.labelTotal.textColors

        // Messe die Höhe/Breite des Dashboards und teile sie dem ViewModel mit
        binding.headerContainer.doOnLayout {
            val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape) {
                sharedViewModel.setHeaderHeight(0) // Im Querformat liegen sie nebeneinander
            } else {
                sharedViewModel.setHeaderHeight(it.height)
            }
        }

        sharedViewModel.selectedProfile.observe(viewLifecycleOwner) { aircraftProfile ->
            updateUiForSelectedProfile(aircraftProfile)
        }

        setupCalculationObservers()
    }

    private fun updateUiForSelectedProfile(aircraftProfile: AircraftProfile?) {
        val currentFragment = childFragmentManager.findFragmentById(R.id.bottom_fragment_container)

        if (aircraftProfile == null) {
            // Wenn schon das "Kein Flugzeug"-Fragment da ist, nichts tun
            if (currentFragment is NoSelectedAircraftFragment) return
            
            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.bottom_fragment_container, NoSelectedAircraftFragment())
                .commit()
        } else {
            // WICHTIG: Wenn das ScrollingFragment bereits da ist, prüfen wir im Tag oder ViewModel,
            // ob es das gleiche Flugzeug ist. Da das ScrollingFragment ohnehin auf das 
            // SharedViewModel hört, müssen wir es NUR austauschen, wenn es noch gar nicht da ist.
            if (currentFragment is ScrollingFragment) return
            
            childFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.bottom_fragment_container, ScrollingFragment())
                .commit()
        }
    }

    private fun setupCalculationObservers() {
        // 1. Gesamtgewicht Observer
        sharedViewModel.totalMass.observe(viewLifecycleOwner) { result ->
            val aircraft = sharedViewModel.selectedProfile.value?.aircraft
            val maxMass = aircraft?.maxTotalMass ?: 0.0
            val hasLimit = aircraft?.maxTotalMass != null
            val wingArea = aircraft?.wingArea ?: 0.0

            when (result) {
                is CalculationResult.Success -> {
                    val value = result.value
                    binding.twGesamtgewichtOutput.text = String.format(Locale.getDefault(), "%.1f kg", value)

                    // Flächenbelastung berechnen und anzeigen
                    if (wingArea > 0) {
                        val loading = value / wingArea
                        binding.twWingLoading.text = getString(R.string.wing_loading_format, loading)
                        binding.twWingLoading.visibility = View.VISIBLE
                    } else {
                        binding.twWingLoading.visibility = View.GONE
                    }

                    // Progress & Validierung
                    val isOutsideLimits = maxMass > 0.0 && value > maxMass
                    val progress = if (maxMass > 0) (value / maxMass * 100).toInt() else 0

                    updateCardVisuals(
                        binding.cardTotalMass,
                        binding.progressTotal,
                        binding.statusTotal,
                        binding.cardStatusTotal,
                        progress,
                        isOutsideLimits,
                        isError = false,
                        hasLimit
                    )
                }
                is CalculationResult.Error -> {
                    binding.twGesamtgewichtOutput.text = "---"
                    binding.twWingLoading.visibility = View.GONE
                    binding.statusTotal.text = getString(R.string.status_error)

                    updateCardVisuals(
                        binding.cardTotalMass,
                        binding.progressTotal,
                        binding.statusTotal,
                        binding.cardStatusTotal,
                        progressValue = null,
                        isOutsideLimits = false,
                        isError = true,
                        hasLimit
                    )
                }
            }
        }

        // 2. Schwerpunkt Observer
        sharedViewModel.cg.observe(viewLifecycleOwner) {
            updateCgUi()
        }

        sharedViewModel.cgRange.observe(viewLifecycleOwner) {
            updateCgUi()
        }

        sharedViewModel.nonLiftingMass.observe(viewLifecycleOwner) { result ->
            val aircraft = sharedViewModel.selectedProfile.value?.aircraft
            val maxNonLifting = aircraft?.maxNonLiftingMass ?: 0.0
            val hasLimit = aircraft?.maxNonLiftingMass != null

            when (result) {
                is CalculationResult.Success -> {
                    val value = result.value
                    binding.twMasseNTTeileErgebnis.text = String.format(Locale.getDefault(), "%.1f kg", value)

                    val isOutsideLimits = maxNonLifting > 0.0 && value > maxNonLifting
                    val progress = if (maxNonLifting > 0) (value / maxNonLifting * 100).toInt() else 0

                    updateCardVisuals(
                        binding.cardNonLifting,
                        binding.progressNonLifting,
                        binding.statusNonLifting,
                        binding.cardStatusNonLifting,
                        progress,
                        isOutsideLimits,
                        isError = false,
                        hasLimit
                    )
                }
                is CalculationResult.Error -> {
                    binding.twMasseNTTeileErgebnis.text = "---"
                    binding.statusNonLifting.text = getString(R.string.status_error)

                    updateCardVisuals(
                        binding.cardNonLifting,
                        binding.progressNonLifting,
                        binding.statusNonLifting,
                        binding.cardStatusNonLifting,
                        progressValue = null,
                        isOutsideLimits = false,
                        isError = true,
                        hasLimit
                    )
                }
            }
        }
    }

    private fun updateCgUi() {
        val result = sharedViewModel.cg.value ?: return
        val flightRange = sharedViewModel.cgRange.value
        val profile = sharedViewModel.selectedProfile.value?.aircraft ?: return
        
        val minCG = profile.minCg ?: 0.0
        val maxCG = profile.maxCg ?: 0.0
        val totalRange = maxCG - minCG
        val hasLimit = profile.maxCg != null && profile.minCg != null

        when (result) {
            is CalculationResult.Success -> {
                val value = result.value
                binding.twSchwerpunktlageErgebnis.text = String.format(Locale.getDefault(), "%.1f mm", value)

                // Prozentuale Lage (MAC)
                val percentage = if (totalRange > 0) (value - minCG) / totalRange * 100 else 0.0
                binding.twCgPercent.text = String.format(Locale.getDefault(), "(%.1f%%)", percentage)

                // Validierung (muss auch für die gesamte Range im Flug gelten!)
                var isOutsideLimits = (minCG > 0.0 || maxCG > 0.0) && (value !in minCG..maxCG)
                
                // Falls es eine Flug-Range gibt, muss der gesamte Bereich innerhalb der Limits liegen
                var rangeStartPct: Float? = null
                var rangeEndPct: Float? = null
                
                if (flightRange != null) {
                    val (minInFlight, maxInFlight) = flightRange
                    if (minInFlight < minCG || maxInFlight > maxCG) {
                        isOutsideLimits = true
                    }
                    if (totalRange > 0) {
                        rangeStartPct = ((minInFlight - minCG) / totalRange).toFloat()
                        rangeEndPct = ((maxInFlight - minCG) / totalRange).toFloat()
                    }
                }

                // UI Update
                updateCardVisuals(
                    binding.cardCg,
                    binding.progressCg,
                    binding.statusCg,
                    binding.cardStatusCg,
                    percentage.toInt(),
                    isOutsideLimits,
                    isError = false,
                    hasLimit
                )
                
                // Custom Progress Bar Update
                (binding.progressCg as? com.danielschatz.gliderweightbalance.views.CgRangeProgressBar)?.setProgress(
                    (percentage / 100.0).toFloat(),
                    rangeStartPct,
                    rangeEndPct
                )
            }
            is CalculationResult.Error -> {
                binding.twSchwerpunktlageErgebnis.text = "---"
                binding.twCgPercent.text = "---"
                binding.statusCg.text = getString(R.string.status_error)

                updateCardVisuals(
                    binding.cardCg,
                    binding.progressCg,
                    binding.statusCg,
                    binding.cardStatusCg,
                    progressValue = null,
                    isOutsideLimits = false,
                    isError = true,
                    hasLimit
                )
                (binding.progressCg as? com.danielschatz.gliderweightbalance.views.CgRangeProgressBar)?.setProgress(0f, null, null)
            }
        }
    }

    /**
     * Zentralisierte Funktion zum Stylen der einzelnen Dashboard-Karten
     */
    private fun updateCardVisuals(
        card: com.google.android.material.card.MaterialCardView,
        progressIndicator: View,
        statusLabel: android.widget.TextView,
        statusCard: com.google.android.material.card.MaterialCardView,
        progressValue: Int?,
        isOutsideLimits: Boolean,
        isError: Boolean,
        limitExists: Boolean
    ) {
        // 1. Logik für Sichtbarkeit der ProgressBar
        if (isError || !limitExists || progressValue == null) {
            progressIndicator.visibility = View.INVISIBLE
        } else {
            progressIndicator.visibility = View.VISIBLE
            if (progressIndicator is com.google.android.material.progressindicator.LinearProgressIndicator) {
                progressIndicator.progress = progressValue.coerceIn(0, 100)
            }
        }

        // 2. Styling basierend auf dem Zustand (Priorität: Error > OutsideLimits > OK)
        if (isError || isOutsideLimits || !limitExists) {
            // FEHLER-STYLING (ROT)
            val colorRed = getThemeColor(com.google.android.material.R.attr.colorOnErrorContainer)
            val bgRed = getThemeColor(com.google.android.material.R.attr.colorErrorContainer)

            card.setCardBackgroundColor(ColorStateList.valueOf(bgRed))
            card.setStrokeColor(ColorStateList.valueOf(colorRed))
            card.strokeWidth = 8
            card.cardElevation = 0f
            
            if (progressIndicator is com.google.android.material.progressindicator.LinearProgressIndicator) {
                progressIndicator.setIndicatorColor(colorRed)
            } else if (progressIndicator is com.danielschatz.gliderweightbalance.views.CgRangeProgressBar) {
                progressIndicator.setIndicatorColor(colorRed)
            }

            statusLabel.text = when {
                isError -> getString(R.string.status_error)
                !limitExists -> getString(R.string.status_limit_missing)
                else -> getString(R.string.status_out_of_limits)
            }

            statusLabel.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnError))
            statusCard.setCardBackgroundColor(getThemeColor(androidx.appcompat.R.attr.colorError))
            statusCard.strokeWidth = 0

        } else {
            // Karte zurücksetzen
            val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
            card.setCardBackgroundColor(ColorStateList.valueOf(surfaceColor))
            card.strokeWidth = 0
            card.cardElevation = 4 * resources.displayMetrics.density

            val colorIndicator = getThemeColor(androidx.appcompat.R.attr.colorPrimary)
            val colorTrack = getThemeColor(com.google.android.material.R.attr.colorPrimaryInverse)
            
            if (progressIndicator is com.google.android.material.progressindicator.LinearProgressIndicator) {
                progressIndicator.setIndicatorColor(colorIndicator)
                progressIndicator.trackColor = colorTrack
            } else if (progressIndicator is com.danielschatz.gliderweightbalance.views.CgRangeProgressBar) {
                progressIndicator.setIndicatorColor(colorIndicator)
                progressIndicator.setTrackColor(colorTrack)
            }

            statusLabel.text = getString(R.string.status_ok)
            statusLabel.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer))
            statusCard.setCardBackgroundColor(getThemeColor(com.google.android.material.R.attr.colorPrimaryContainer))
            statusCard.strokeWidth = 2
            statusCard.strokeColor = getThemeColor(com.google.android.material.R.attr.colorOnPrimaryContainer)
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}