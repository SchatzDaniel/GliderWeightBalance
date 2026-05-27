package com.example.weightbalance2

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.weightbalance2.data.model.AircraftProfile
import com.example.weightbalance2.databinding.FragmentHomeBinding
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

        updateHeaderColors()

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
        if (aircraftProfile == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.bottom_fragment_container, NoAircraftSelectedFragment())
                .commit()
        } else {
            childFragmentManager.beginTransaction()
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

            when (result) {
                is CalculationResult.Success -> {
                    val value = result.value
                    binding.twGesamtgewichtOutput.text = String.format(Locale.getDefault(), "%.1f kg", value)

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
        sharedViewModel.cg.observe(viewLifecycleOwner) { result ->
            val profile = sharedViewModel.selectedProfile.value?.aircraft ?: return@observe
            val minCG = profile.minCg ?: 0.0
            val maxCG = profile.maxCg ?: 0.0
            val range = maxCG - minCG
            val hasLimit = profile.maxCg != null && profile.minCg != null

            when (result) {
                is CalculationResult.Success -> {
                    val value = result.value
                    binding.twSchwerpunktlageErgebnis.text = String.format(Locale.getDefault(), "%.1f mm", value)

                    // Prozentuale Lage (MAC)
                    val percentage = if (range > 0) (value - minCG) / range * 100 else 0.0
                    binding.twCgPercent.text = String.format(Locale.getDefault(), "(%.1f%%)", percentage)

                    // Validierung
                    val isOutsideLimits = (minCG > 0.0 || maxCG > 0.0) && (value !in minCG..maxCG)

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
                }
            }
        }

        // 3. Masse n.t.T. Observer
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

    /**
     * Zentralisierte Funktion zum Stylen der einzelnen Dashboard-Karten
     */
    private fun updateCardVisuals(
        card: com.google.android.material.card.MaterialCardView,
        progressIndicator: com.google.android.material.progressindicator.LinearProgressIndicator,
        statusLabel: android.widget.TextView,
        statusCard: com.google.android.material.card.MaterialCardView,
        progressValue: Int?,
        isOutsideLimits: Boolean,
        isError: Boolean,
        limitExists: Boolean
    ) {
        // 1. Logik für Sichtbarkeit und Fortschritt der ProgressBar
        when {
            isError || !limitExists ||progressValue == null -> {
                progressIndicator.visibility = View.INVISIBLE
            }
            else -> {
                progressIndicator.visibility = View.VISIBLE
                progressIndicator.progress = progressValue.coerceIn(0, 100)
            }
        }

        // 2. Styling basierend auf dem Zustand (Priorität: Error > OutsideLimits > OK)
        if (isError || isOutsideLimits || !limitExists) {
            // FEHLER-STYLING (ROT)
            val colorRed = getThemeColor(com.google.android.material.R.attr.colorOnErrorContainer)
            val bgRed = getThemeColor(com.google.android.material.R.attr.colorErrorContainer)

            // Karte Rot färben
            card.setCardBackgroundColor(ColorStateList.valueOf(bgRed))
            card.setStrokeColor(ColorStateList.valueOf(colorRed))
            card.strokeWidth = 8
            card.cardElevation = 0f
            progressIndicator.setIndicatorColor(colorRed)

            // Dynamischer Status-Text
            statusLabel.text = when {
                isError -> getString(R.string.status_error)
                !limitExists -> getString(R.string.status_limit_missing) // Signalisiert: Berechnung ok, aber keine Grenze
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

            // Progress & Status auf OK
            val colorIndicator = getThemeColor(androidx.appcompat.R.attr.colorPrimary)
            val colorTrack = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant)
            
            progressIndicator.setIndicatorColor(colorIndicator)
            progressIndicator.trackColor = colorTrack

            // Statuslabel setzen
            val colorOk = getThemeColor(R.attr.colorPositive)
            statusLabel.text = getString(R.string.status_ok)
            statusLabel.setTextColor(colorOk)
            statusCard.setCardBackgroundColor(getThemeColor(com.google.android.material.R.attr.colorPrimaryContainer))
            statusCard.strokeWidth = 2
            statusCard.setStrokeColor(ColorStateList.valueOf(colorOk))
        }
    }

    private fun getThemeColor(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun updateHeaderColors() {
        val headerColor = getThemeColor(androidx.appcompat.R.attr.colorPrimary)
        
        // Prüfen, ob wir im Dark Mode sind
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        // StatusBar Farbe anpassen
        activity?.window?.let { window ->
            @Suppress("DEPRECATION")
            window.statusBarColor = headerColor
            // Icons dunkel im Light Mode, hell im Dark Mode
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
        }

        // ActionBar/Toolbar Farbe anpassen
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setBackgroundDrawable(headerColor.toDrawable())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}