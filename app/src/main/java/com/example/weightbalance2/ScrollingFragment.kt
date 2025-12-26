package com.example.weightbalance2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.weightbalance2.databinding.FragmentScrollingBinding

class ScrollingFragment : Fragment() {

    private var _binding: FragmentScrollingBinding? = null
    // Diese Property ist nur zwischen onCreateView und onDestroyView gültig.
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

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

        // Hilfsfunktion, um einen Wert nur dann zu setzen, wenn er nicht 0.0 ist
        fun setInputTextIfNotZero(editText: EditText, value: Double?) {
            if (value != null && value != 0.0) {
                editText.setText(value.toString().removeSuffix(".0"))
            } else {
                editText.setText("") // Setze das Feld auf leer, wenn der Wert 0.0 oder null ist
            }
        }

        // Fülle die EditText-Felder mit der neuen Logik
        setInputTextIfNotZero(binding.pilotMassInput, sharedViewModel.pilotMass.value)
        setInputTextIfNotZero(binding.trimPillowMassInput, sharedViewModel.trimPillowMass.value)
        setInputTextIfNotZero(binding.trimBallastMassInput, sharedViewModel.trimBallastMass.value)
        setInputTextIfNotZero(binding.cockpitBaggageMassInput, sharedViewModel.cockpitBaggageMass.value)
        setInputTextIfNotZero(binding.parachuteMassInput, sharedViewModel.parachuteMass.value)
        setInputTextIfNotZero(binding.lowerBaggageMassInput, sharedViewModel.lowerBaggageMass.value)
        setInputTextIfNotZero(binding.upperBaggageMassInput, sharedViewModel.upperBaggageMass.value)
        setInputTextIfNotZero(binding.waterBallastMassInput, sharedViewModel.waterBallastMass.value)
        setInputTextIfNotZero(binding.stabilizerBallastMassInput, sharedViewModel.stabilizerBallastMass.value)
        setInputTextIfNotZero(binding.oxygenMassInput, sharedViewModel.oxygenMass.value)
        setInputTextIfNotZero(binding.instrumentMassInput, sharedViewModel.instrumentMass.value)

        binding.pilotMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.pilotMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        binding.cockpitBaggageMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.cockpitBaggageMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        binding.trimBallastMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.trimBallastMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        binding.trimPillowMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.trimPillowMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        binding.parachuteMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.parachuteMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        binding.lowerBaggageMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.lowerBaggageMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        binding.upperBaggageMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.upperBaggageMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        binding.waterBallastMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.waterBallastMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        binding.stabilizerBallastMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.stabilizerBallastMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        binding.oxygenMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.oxygenMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }
        binding.instrumentMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.instrumentMass.value = text.toString().toDoubleOrNull() ?: 0.0
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Setze das Binding zurück, um Memory Leaks zu vermeiden.
        _binding = null
    }
}
