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
        liveData.observe(viewLifecycleOwner) { value ->
            editText.removeTextChangedListener(textWatchers[editText])

            // Korrektur: Wir müssen den Text im Feld genauso behandeln wie die Eingabe des Benutzers.
            val currentText = editText.text.toString().replace(',', '.')
            val currentValue = currentText.toDoubleOrNull() ?: 0.0

            // Wir aktualisieren die UI nur, wenn der *numerische Wert* abweicht.
            // Das verhindert, dass "75." und "75" als unterschiedlich gelten.
            if (currentValue != value) {
                val newText = when {
                    value == 0.0 -> ""
                    // Wandle für die Anzeige den Punkt wieder in ein Komma um (je nach lokaler Konvention).
                    // Dies ist benutzerfreundlicher für deutsche Nutzer.
                    else -> value.toString().replace('.', ',').removeSuffix(",0")
                }
                editText.setText(newText)
                editText.setSelection(newText.length)
            }

            textWatchers[editText]?.let { editText.addTextChangedListener(it) }
        }

        // RICHTUNG 2: UI -> ViewModel
        val textWatcher = editText.doAfterTextChanged { text ->
            val textAsString = text.toString()
            // KORREKTUR: Wir behandeln das Komma und erlauben Eingaben, die mit "." oder "," enden.
            val correctedText = textAsString.replace(',', '.')

            // Wenn der Text nur "0" ist, aber der Nutzer "0." oder "0," eingeben will,
            // dann ist der numerische Wert 0.0, aber die Eingabe soll respektiert werden.
            if (textAsString.isEmpty() && liveData.value != 0.0) {
                liveData.value = 0.0
            } else if (correctedText.isNotEmpty() && correctedText.last() != '.') {
                // Konvertiere nur, wenn die Eingabe nicht auf einen Punkt endet.
                liveData.value = correctedText.toDoubleOrNull() ?: (liveData.value ?: 0.0)
            } else if (correctedText.isEmpty()) {
                liveData.value = 0.0
            }
        }
        textWatchers[editText] = textWatcher
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Setze das Binding und die Watcher-Map zurück, um Memory Leaks zu vermeiden.
        textWatchers.clear()
        _binding = null
    }
}
