package com.example.weightbalance2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import com.example.weightbalance2.databinding.FragmentScrollingBinding

class ScrollingFragment : Fragment() {

    private var _binding: FragmentScrollingBinding? = null
    // Diese Property ist nur zwischen onCreateView und onDestroyView gültig.
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by navGraphViewModels(R.id.main_nav)

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

        // Fülle die EditText-Felder mit den aktuellen Werten aus dem ViewModel
        binding.pilotMassInput.setText(sharedViewModel.pilotMass.value?.toString())
        binding.cockpitBaggageMassInput.setText(sharedViewModel.cockpitBaggageMass.value?.toString())
        binding.trimBallastMassInput.setText(sharedViewModel.trimBallastMass.value?.toString())
        binding.trimPillowMassInput.setText(sharedViewModel.trimPillowMass.value?.toString())
        binding.parachuteMassInput.setText(sharedViewModel.parachuteMass.value?.toString())
        binding.lowerBaggageMassInput.setText(sharedViewModel.lowerBaggageMass.value?.toString())
        binding.upperBaggageMassInput.setText(sharedViewModel.upperBaggageMass.value?.toString())
        binding.waterBallastMassInput.setText(sharedViewModel.waterBallastMass.value?.toString())
        binding.stabilizerBallastMassInput.setText(sharedViewModel.stabilizerBallastMass.value?.toString())
        binding.oxygenMassInput.setText(sharedViewModel.oxygenMass.value?.toString())
        binding.instrumentMassInput.setText(sharedViewModel.instrumentMass.value?.toString())

        binding.pilotMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.pilotMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }
        binding.cockpitBaggageMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.cockpitBaggageMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }
        binding.trimBallastMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.trimBallastMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }
        binding.trimPillowMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.trimPillowMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }
        binding.parachuteMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.parachuteMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }
        binding.lowerBaggageMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.lowerBaggageMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }
        binding.upperBaggageMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.upperBaggageMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }
        binding.waterBallastMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.waterBallastMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }
        binding.stabilizerBallastMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.stabilizerBallastMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }
        binding.oxygenMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.oxygenMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }
        binding.instrumentMassInput.doAfterTextChanged { text ->
            // Setze den Wert im ViewModel. Gib 0.0 zurück, wenn die Eingabe leer oder ungültig ist.
            sharedViewModel.instrumentMass.value = text.toString().toDoubleOrNull() ?: 0.0
            sharedViewModel.recalc()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Setze das Binding zurück, um Memory Leaks zu vermeiden.
        _binding = null
    }
}
