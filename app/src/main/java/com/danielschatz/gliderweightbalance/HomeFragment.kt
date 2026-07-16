package com.danielschatz.gliderweightbalance

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MediatorLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.danielschatz.gliderweightbalance.adapter.CalculationsAdapter
import com.danielschatz.gliderweightbalance.databinding.FragmentHomeBinding
import com.danielschatz.gliderweightbalance.views.EnvelopeView
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.transition.Fade
import android.transition.ChangeBounds
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar
import androidx.preference.PreferenceManager
import android.widget.LinearLayout
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var defaultTextColor: ColorStateList? = null
    private lateinit var calculationsAdapter: CalculationsAdapter
    private var focusChangeListener: android.view.ViewTreeObserver.OnGlobalFocusChangeListener? = null

    private var simulationJob: Job? = null
    private var isHintSnackbarShowing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupSimulationControls()
        setupExtremeScenarioControls()
        
        defaultTextColor = binding.labelTotal.textColors

        binding.headerContainer.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val height = if (isLandscape) 0 else v.height
            if (sharedViewModel.headerHeight.value != height) {
                sharedViewModel.setHeaderHeight(height)
            }
        }

        setupKeyboardHandling()

        binding.cardCg.setOnClickListener {
            toggleSimulationMode()
        }
    }

    private fun setupExtremeScenarioControls() {
        val container = binding.root.findViewById<LinearLayout>(R.id.containerExtremeScenarios) ?: return
        
        sharedViewModel.extremeStates.observe(viewLifecycleOwner) { states ->
            if (states == null) return@observe
            
            container.removeAllViews()
            states.forEach { state ->
                val cardBinding = com.danielschatz.gliderweightbalance.databinding.ItemExtremeScenarioCardBinding.inflate(
                    LayoutInflater.from(requireContext()), container, false
                )
                
                cardBinding.tvScenarioTitle.text = when (state.type) {
                    ExtremeState.Type.FORWARD -> getString(R.string.extreme_state_forward)
                    ExtremeState.Type.TAKE_OFF -> getString(R.string.extreme_state_take_off)
                    ExtremeState.Type.AFT -> getString(R.string.extreme_state_aft)
                }
                
                cardBinding.tvScenarioCg.text = String.format(Locale.getDefault(), "%.1f mm", state.cgLocation)
                
                // Details: Nur veränderbare Tanks anzeigen
                val profile = sharedViewModel.selectedProfile.value
                val details = state.stationMasses.mapNotNull { (id, mass) ->
                    val station = profile?.stations?.find { it.station.stationId == id }?.station
                    if (station?.isConsumable == true) {
                        "${station.name}: ${String.format(Locale.getDefault(), "%.1f", mass)}${station.unit ?: "kg"}"
                    } else null
                }.joinToString(" | ")
                
                cardBinding.tvScenarioDetails.text = details
                
                // Selektions-Status (Outline)
                sharedViewModel.currentSimulationState.observe(viewLifecycleOwner) { simState ->
                    val isSelected = simState?.cgLocation == state.cgLocation && simState?.totalMass == state.totalMass
                    cardBinding.cardScenario.strokeWidth = if (isSelected) {
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics).toInt()
                    } else 0
                }

                cardBinding.cardScenario.setOnClickListener {
                    sharedViewModel.selectExtremeState(state)
                    calculationsAdapter.setSimulationValues(state.stationMasses)
                }
                
                container.addView(cardBinding.root)
            }
        }
    }

    private fun setupSimulationControls() {
        val simContainer = binding.root.findViewById<View>(R.id.simulationContainer)
        val slider = simContainer.findViewById<Slider>(R.id.simulationSlider)
        val btnPlay = simContainer.findViewById<MaterialButton>(R.id.btnPlaySimulation)
        val btnApply = simContainer.findViewById<MaterialButton>(R.id.btnApplySimulation)
        val tvTime = simContainer.findViewById<android.widget.TextView>(R.id.tvSimulationTime)

        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                sharedViewModel.setSimulationTime(value.toDouble())
            }
        }

        btnPlay.setOnClickListener {
            if (simulationJob?.isActive == true) {
                stopSimulation()
            } else {
                startSimulation(slider)
            }
        }

        // Falls noch nicht registriert, UI Button initial setzen
        btnApply.setIconResource(R.drawable.ic_refresh)

        btnApply.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_apply_simulation_title)
                .setMessage(R.string.dialog_apply_simulation_message)
                .setPositiveButton(R.string.save) { dialog, _ ->
                    sharedViewModel.applySimulationToState()
                    toggleSimulationMode() // Diagramm schließen
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        sharedViewModel.currentSimulationState.observe(viewLifecycleOwner) { state ->
            if (state != null) {
                slider.stepSize = 0f // Kontinuierlicher Slider
                slider.valueTo = state.maxDuration.toFloat().coerceAtLeast(1f)
                
                // Slider nur nachziehen, wenn der Nutzer ihn NICHT gerade selbst bewegt
                if (!slider.isFocused) {
                    slider.value = state.timeSeconds.toFloat()
                }
                
                val minutes = state.timeSeconds.toInt() / 60
                val seconds = state.timeSeconds.toInt() % 60
                tvTime.text = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
                
                // Envelope View aktualisieren
                val envelopeView = binding.root.findViewById<EnvelopeView>(R.id.envelopeView)
                envelopeView.setSimulationPath(sharedViewModel.simulationPath.value ?: emptyList(), state.cgLocation to state.totalMass)
                
                // Falls Simulation aktiv, Liste updaten
                if (sharedViewModel.isSimulationActive.value == true) {
                    calculationsAdapter.setSimulationValues(state.stationMasses)
                }
            }
        }
    }

    private fun startSimulation(slider: Slider) {
        val btnPlay = binding.root.findViewById<MaterialButton>(R.id.btnPlaySimulation)
        btnPlay.setIconResource(R.drawable.ic_pause)
        
        simulationJob = viewLifecycleOwner.lifecycleScope.launch {
            var currentTime = sharedViewModel.simulationTime.value ?: 0.0
            val maxTime = slider.valueTo.toDouble()
            
            if (currentTime >= maxTime) currentTime = 0.0

            while (isActive && currentTime < maxTime) {
                currentTime += 1.0 // Echtzeit: 1 Sekunde pro Update
                sharedViewModel.setSimulationTime(currentTime)
                delay(1000) // Update jede Sekunde
            }
            stopSimulation()
        }
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        val btnPlay = binding.root.findViewById<MaterialButton>(R.id.btnPlaySimulation)
        btnPlay.setIconResource(R.drawable.ic_play)
    }

    private fun toggleSimulationMode() {
        val isActive = sharedViewModel.isSimulationActive.value ?: false
        val newActive = !isActive
        
        val simContainer = binding.root.findViewById<View>(R.id.simulationContainer)
        val lowerCards = binding.root.findViewById<View>(R.id.lowerCardsContainer)
        
        val layoutSingle = binding.root.findViewById<View>(R.id.layoutSimSingleGroup)
        val layoutMulti = binding.root.findViewById<View>(R.id.layoutSimMultiGroup)
        val cardNoConsumables = binding.root.findViewById<View>(R.id.cardNoConsumablesInfo)
        
        val numGroups = sharedViewModel.numOperationalGroups.value ?: 0
        val useSingleMode = numGroups == 1
        val noConsumables = numGroups == 0
        val wasVisible = simContainer.isVisible

        // 1. Vorbereiten der Simulation
        sharedViewModel.setSimulationActive(newActive)
        calculationsAdapter.setSimulationMode(newActive)
        
        // 2. Ziel-Höhe berechnen
        layoutSingle.isVisible = useSingleMode || noConsumables
        layoutMulti.isVisible = !useSingleMode && !noConsumables
        cardNoConsumables.isVisible = noConsumables

        simContainer.isVisible = newActive
        lowerCards.isVisible = !newActive
        
        binding.headerContainer.measure(
            View.MeasureSpec.makeMeasureSpec(binding.headerContainer.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = binding.headerContainer.measuredHeight
        
        // Sichtbarkeit für die Transition kurz zurücksetzen
        simContainer.isVisible = wasVisible
        lowerCards.isVisible = !wasVisible

        // 3. Sequenzielle Transition definieren
        val transition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_SEQUENTIAL
            
            // Schritt A: Altes ausfaden + Größe anpassen (Gleichzeitig)
            addTransition(TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                addTransition(Fade(Fade.OUT).setDuration(150))
                addTransition(ChangeBounds().setDuration(300))
            })
            
            // Schritt B: Neues einfaden
            addTransition(Fade(Fade.IN).setDuration(200))
        }

        TransitionManager.beginDelayedTransition(binding.root as ViewGroup, transition)
        
        // 4. Jetzt die tatsächlichen Änderungen durchführen
        simContainer.isVisible = newActive
        lowerCards.isVisible = !newActive

        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        sharedViewModel.setHeaderHeight(if (isLandscape) 0 else targetHeight)

        if (newActive) {
            val aircraft = sharedViewModel.selectedProfile.value?.aircraft ?: return
            
            val envelopeView = binding.root.findViewById<EnvelopeView>(R.id.envelopeView)
            envelopeView.setData(
                minCg = aircraft.minCg ?: 0.0,
                maxCg = aircraft.maxCg ?: 0.0,
                emptyMass = aircraft.emptyWeight ?: 0.0,
                maxMass = aircraft.maxTotalMass ?: 0.0
            )

            // Farbe der Envelope-Karte explizit an die anderen Dashboard-Karten angleichen
            binding.root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardEnvelope)?.let { card ->
                val surfaceColor = getThemeColor(com.google.android.material.R.attr.colorSurface)
                card.setCardBackgroundColor(ColorStateList.valueOf(surfaceColor))
                card.cardElevation = 4 * resources.displayMetrics.density
                card.strokeWidth = 0
            }
        } else {
            stopSimulation()
            calculationsAdapter.clearSimulationValues()
        }
    }

    private fun setupKeyboardHandling() {
        focusChangeListener = android.view.ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus is android.widget.EditText) {
                scrollToFocusedView(newFocus)
            }
        }
        binding.root.viewTreeObserver.addOnGlobalFocusChangeListener(focusChangeListener)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                _binding?.let { it.root.findFocus()?.let { view -> scrollToFocusedView(view) } }
            }
            insets
        }
    }

    private fun scrollToFocusedView(view: View) {
        val binding = _binding ?: return
        binding.recyclerViewMassInputs.post {
            val currentBinding = _binding ?: return@post
            val rect = android.graphics.Rect()
            view.getDrawingRect(rect)
            
            try {
                currentBinding.recyclerViewMassInputs.offsetDescendantRectToMyCoords(view, rect)
            } catch (_: Exception) {
                return@post
            }

            val headerHeight = sharedViewModel.headerHeight.value ?: 0
            val rvHeight = currentBinding.recyclerViewMassInputs.height
            val paddingBottom = currentBinding.recyclerViewMassInputs.paddingBottom
            
            val visibleTop = headerHeight
            val visibleBottom = rvHeight - paddingBottom

            if (rect.bottom > visibleBottom) {
                val scrollAmount = rect.bottom - visibleBottom + 24
                currentBinding.recyclerViewMassInputs.smoothScrollBy(0, scrollAmount)
            } else if (rect.top < visibleTop) {
                val scrollAmount = rect.top - visibleTop - 24
                currentBinding.recyclerViewMassInputs.smoothScrollBy(0, scrollAmount)
            }
        }
    }

    private fun setupRecyclerView() {
        calculationsAdapter = CalculationsAdapter { stationId, newWeight, selectedPreset, amount, isSlider ->
            sharedViewModel.updateStationState(stationId, newWeight, selectedPreset, amount, isSlider)
        }

        binding.recyclerViewMassInputs.apply {
            adapter = calculationsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null
        }
    }

    private fun observeViewModel() {
        sharedViewModel.headerHeight.observe(viewLifecycleOwner) { height ->
            val extraOffset = if (height > 0) {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()
            } else 0
            
            val newTopPadding = height + extraOffset
            val oldTopPadding = binding.recyclerViewMassInputs.paddingTop
            
            // Wenn die Liste ganz oben ist, wollen wir den Scroll-Zustand beibehalten
            val isAtTop = !binding.recyclerViewMassInputs.canScrollVertically(-1)

            binding.recyclerViewMassInputs.setPadding(
                binding.recyclerViewMassInputs.paddingLeft,
                newTopPadding,
                binding.recyclerViewMassInputs.paddingRight,
                binding.recyclerViewMassInputs.paddingBottom
            )

            if (isAtTop && oldTopPadding != newTopPadding) {
                binding.recyclerViewMassInputs.scrollToPosition(0)
            }
        }

        sharedViewModel.onScenarioApplied.observe(viewLifecycleOwner) {
            calculationsAdapter.notifyItemRangeChanged(0, calculationsAdapter.itemCount, "SCENARIO_APPLIED")
        }

        sharedViewModel.selectedProfile.observe(viewLifecycleOwner) { profile ->
            if (profile == null) {
                binding.recyclerViewMassInputs.isVisible = false
                binding.textViewNoAircraftSelected.isVisible = true
                calculationsAdapter.submitList(emptyList())
            } else {
                binding.recyclerViewMassInputs.isVisible = true
                binding.textViewNoAircraftSelected.isVisible = false
                calculationsAdapter.updateData(profile.stations)
            }
        }

        setupCalculationObservers()
    }

    private fun setupCalculationObservers() {
        val totalMassStatusMediator = MediatorLiveData<Pair<CalculationResult?, Boolean>>()
        totalMassStatusMediator.addSource(sharedViewModel.totalMass) { totalMassStatusMediator.value = it to (sharedViewModel.isAnyStationOverloaded.value ?: false) }
        totalMassStatusMediator.addSource(sharedViewModel.isAnyStationOverloaded) { totalMassStatusMediator.value = (sharedViewModel.totalMass.value) to it }

        totalMassStatusMediator.observe(viewLifecycleOwner) { (result, isStationOverLimit) ->
            if (result == null) return@observe
            
            val aircraft = sharedViewModel.selectedProfile.value?.aircraft
            val maxMass = aircraft?.maxTotalMass ?: 0.0
            val hasLimit = aircraft?.maxTotalMass != null
            val wingArea = aircraft?.wingArea ?: 0.0

            when (result) {
                is CalculationResult.Success -> {
                    val value = result.value
                    binding.twGesamtgewichtOutput.text = String.format(Locale.getDefault(), "%.1f kg", value)

                    if (wingArea > 0) {
                        val loading = value / wingArea
                        binding.twWingLoading.text = getString(R.string.wing_loading_format, loading)
                        binding.twWingLoading.visibility = View.VISIBLE
                    } else {
                        binding.twWingLoading.visibility = View.GONE
                    }

                    val isOutsideMTOW = maxMass > 0.0 && value > maxMass
                    val progress = if (maxMass > 0) (value / maxMass * 100).toInt() else 0

                    updateCardVisuals(
                        binding.cardTotalMass,
                        binding.progressTotal,
                        binding.statusTotal,
                        binding.cardStatusTotal,
                        progress,
                        isOutsideLimits = isOutsideMTOW,
                        isError = false,
                        limitExists = hasLimit,
                        isStationOverLimit = isStationOverLimit
                    )
                }
                is CalculationResult.Error -> {
                    binding.twGesamtgewichtOutput.text = "---"
                    binding.twWingLoading.visibility = View.GONE
                    updateCardVisuals(
                        binding.cardTotalMass,
                        binding.progressTotal,
                        binding.statusTotal,
                        binding.cardStatusTotal,
                        null,
                        false,
                        isError = true,
                        hasLimit,
                        isStationOverLimit = false
                    )
                }
            }
        }

        sharedViewModel.cg.observe(viewLifecycleOwner) {
            updateCgUi()
        }

        sharedViewModel.cgRange.observe(viewLifecycleOwner) { range ->
            updateCgUi()
            
            // Simulation-Icon anzeigen, wenn eine Verlagerung berechnet wurde
            val hasMigration = range != null && (range.second - range.first) > 0.1
            binding.root.findViewById<android.widget.ImageView>(R.id.ivSimulationHint)?.isVisible = hasMigration
            
            // Snackbar beim ersten Mal anzeigen
            if (hasMigration) {
                showSimulationHintOnce()
            }
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

    private fun showSimulationHintOnce() {
        if (!isAdded || isHintSnackbarShowing) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        
        // DEBUGGING: Aktiviere die nächste Zeile, um den Hinweis bei jedem App-Start zu sehen:
        // prefs.edit().putBoolean("hint_simulation_shown", false).apply()

        val hasShown = prefs.getBoolean("hint_simulation_shown", false)
        
        if (!hasShown) {
            val rootView = activity?.findViewById<View>(android.R.id.content) ?: binding.root
            val bottomNav = activity?.findViewById<View>(R.id.bottomNavigation)
            
            isHintSnackbarShowing = true
            Snackbar.make(rootView, R.string.hint_simulation_available, Snackbar.LENGTH_INDEFINITE)
                .apply {
                    if (bottomNav?.visibility == View.VISIBLE) {
                        anchorView = bottomNav
                    }
                }
                .setAction(android.R.string.ok) {
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putBoolean("hint_simulation_shown", true).apply()
                    isHintSnackbarShowing = false
                }
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        if (event != DISMISS_EVENT_ACTION) {
                            isHintSnackbarShowing = false
                        }
                    }
                })
                .show()
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
        val isSimulation = sharedViewModel.isSimulationActive.value ?: false

        when (result) {
            is CalculationResult.Success -> {
                val value = result.value
                binding.twSchwerpunktlageErgebnis.text = String.format(Locale.getDefault(), "%.1f mm", value)

                val percentage = if (totalRange > 0) (value - minCG) / totalRange * 100 else 0.0
                binding.twCgPercent.text = String.format(Locale.getDefault(), "(%.1f%%)", percentage)

                var isOutsideLimits = (minCG > 0.0 || maxCG > 0.0) && (value !in minCG..maxCG)
                
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
                
                // Animation deaktivieren, wenn wir in der Simulation sind
                binding.progressCg.setProgress(
                    (percentage / 100.0).toFloat(),
                    rangeStartPct,
                    rangeEndPct,
                    if (isSimulation) false else (sharedViewModel.shouldAnimate.value ?: true)
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
                binding.progressCg.setProgress(0f, null, null)
            }
        }
    }

    private fun updateCardVisuals(
        card: com.google.android.material.card.MaterialCardView,
        progressIndicator: View,
        statusLabel: android.widget.TextView,
        statusCard: com.google.android.material.card.MaterialCardView,
        progressValue: Int?,
        isOutsideLimits: Boolean,
        isError: Boolean,
        limitExists: Boolean,
        isStationOverLimit: Boolean = false
    ) {
        val isSimulation = sharedViewModel.isSimulationActive.value ?: false
        val animated = if (isSimulation) false else (sharedViewModel.shouldAnimate.value ?: true)

        if (isError || !limitExists || progressValue == null) {
            progressIndicator.visibility = View.INVISIBLE
        } else {
            progressIndicator.visibility = View.VISIBLE
            if (progressIndicator is com.google.android.material.progressindicator.LinearProgressIndicator) {
                if (animated) {
                    progressIndicator.setProgress(progressValue.coerceIn(0, 100), true)
                } else {
                    progressIndicator.progress = progressValue.coerceIn(0, 100)
                }
            }
        }

        if (isError || isOutsideLimits || !limitExists || isStationOverLimit) {
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
                isStationOverLimit -> {
                    statusLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    getString(R.string.status_station_overload)
                }
                !limitExists -> getString(R.string.status_limit_missing)
                else -> getString(R.string.status_out_of_limits)
            }

            if (!isStationOverLimit) {
                val defaultSize = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 11f else 14f
                statusLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, defaultSize)
            }

            statusLabel.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnError))
            statusCard.setCardBackgroundColor(getThemeColor(androidx.appcompat.R.attr.colorError))
            statusCard.strokeWidth = 0

        } else {
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
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.root?.viewTreeObserver?.removeOnGlobalFocusChangeListener(focusChangeListener)
        _binding = null
    }
}
