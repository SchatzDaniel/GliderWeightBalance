package com.example.weightbalance2

import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.example.weightbalance2.databinding.FragmentScrollingBinding

class ScrollingFragment : Fragment() {

    private var _binding: FragmentScrollingBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    // Speichert die TextWatcher, damit wir sie entfernen und hinzufügen können,
    // um Endlosschleifen beim programmgesteuerten Ändern von Text zu vermeiden.
    private val textWatchers = mutableMapOf<EditText, TextWatcher>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScrollingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Da dieses Fragment nur noch angezeigt wird, wenn ein Flugzeug ausgewählt ist,
        // entfällt der Observer zur Steuerung der Sichtbarkeit.

        // Binde jedes Eingabefeld an seine eigene LiveData-Quelle.
        // Dies stellt sicher, dass die UI immer die Daten aus dem ViewModel widerspiegelt.
        bindInput(binding.pilotMassInput, sharedViewModel.pilotMass)
        bindInput(binding.trimPillowMassInput, sharedViewModel.trimPillowMass)
        bindInput(binding.trimBallastMassInput, sharedViewModel.trimBallastMass)
        bindInput(binding.cockpitBaggageMassInput, sharedViewModel.cockpitBaggageMass)
        bindInput(binding.parachuteMassInput, sharedViewModel.parachuteMass)
        bindInput(binding.lowerBaggageMassInput, sharedViewModel.lowerBaggageMass)
        bindInput(binding.upperBaggageMassInput, sharedViewModel.upperBaggageMass)
        bindInput(binding.waterBallastMassInput, sharedViewModel.waterBallastMass)
        bindInput(binding.stabilizerBallastMassInput, sharedViewModel.stabilizerBallastMass)
        bindInput(binding.oxygenMassInput, sharedViewModel.oxygenMass)
        bindInput(binding.instrumentMassInput, sharedViewModel.instrumentMass)
    }

    /**
     * Eine wiederverwendbare Funktion, die ein EditText an ein MutableLiveData<Double> bindet.
     * Sie kümmert sich um die bidirektionale Aktualisierung.
     */
    private fun bindInput(editText: EditText, liveData: MutableLiveData<Double>) {
        // RICHTUNG 1: ViewModel -> UI
        // Aktualisiere das EditText-Feld, wenn sich die LiveData ändert.
        liveData.observe(viewLifecycleOwner) { value ->
            // Entferne den Listener vorübergehend, um eine Endlosschleife zu verhindern.
            editText.removeTextChangedListener(textWatchers[editText])

            val currentText = editText.text.toString()
            // Konvertiere den Wert aus dem ViewModel in einen darstellbaren String.
            val newText = when {
                value == 0.0 -> "" // Zeige nichts an, wenn der Wert 0.0 ist.
                else -> value.toString().removeSuffix(".0") // Entferne ".0" für Ganzzahlen.
            }

            // Aktualisiere das Feld nur, wenn sich der Text wirklich geändert hat.
            if (currentText != newText) {
                editText.setText(newText)
                // Setze den Cursor am Ende, um ein Springen zu verhindern.
                editText.setSelection(newText.length)
            }

            // Füge den Listener wieder hinzu, damit Nutzereingaben wieder erfasst werden.
            textWatchers[editText]?.let { editText.addTextChangedListener(it) }
        }

        // RICHTUNG 2: UI -> ViewModel
        // Aktualisiere die LiveData, wenn der Benutzer im EditText etwas ändert.
        val textWatcher = editText.doAfterTextChanged { text ->
            // Konvertiere die Nutzereingabe in ein Double und speichere es im ViewModel.
            // Ein leeres Feld wird zu 0.0.
            liveData.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        // Speichere den TextWatcher, um ihn oben entfernen zu können.
        textWatchers[editText] = textWatcher
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Setze das Binding und die Watcher-Map zurück, um Memory Leaks zu vermeiden.
        textWatchers.clear()
        _binding = null
    }
}
