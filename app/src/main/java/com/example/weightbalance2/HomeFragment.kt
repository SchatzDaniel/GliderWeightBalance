package com.example.weightbalance2

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.weightbalance2.databinding.FragmentHomeBinding
import java.math.RoundingMode

class HomeFragment : Fragment(){

    lateinit var navController: NavController

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by navGraphViewModels(R.id.main_nav)

    // Variable, um die Standard-Textfarbe zu speichern
    private var defaultTextColor: ColorStateList? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        defaultTextColor = binding.twGesamtgewichtOutput.textColors

        sharedViewModel.totalMass.observe(viewLifecycleOwner) {
            binding.twGesamtgewichtOutput.text =
                it.toBigDecimal().setScale(1, RoundingMode.UP).toDouble().toString()
            val maxMass = sharedViewModel.maxTotalMass.value ?: 0.0
            if (it > maxMass) {
                binding.twGesamtgewichtOutput.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error_text_color)
                )
                binding.twGesamtgewichtOutput.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.error_background_color)
                )
            }
            else {
                binding.twGesamtgewichtOutput.setTextColor(defaultTextColor)
                binding.twGesamtgewichtOutput.setBackgroundResource(android.R.color.transparent)
            }
        }
        sharedViewModel.cg.observe(viewLifecycleOwner) {
            binding.twSchwerpunktlageErgebnis.text =
                it.toBigDecimal().setScale(1, RoundingMode.UP).toDouble().toString()
            val minCG = sharedViewModel.minCG.value ?: 0.0
            val maxCG = sharedViewModel.maxCG.value ?: 0.0
            if (it < minCG || it > maxCG) {
                binding.twSchwerpunktlageErgebnis.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error_text_color)
                )
                binding.twSchwerpunktlageErgebnis.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.error_background_color)
                )
            }
            else {
                binding.twSchwerpunktlageErgebnis.setTextColor(defaultTextColor)
                binding.twSchwerpunktlageErgebnis.setBackgroundResource(android.R.color.transparent)
            }
        }
        sharedViewModel.nonLiftingMass.observe(viewLifecycleOwner) {
            binding.twMasseNTTeileErgebnis.text =
                it.toBigDecimal().setScale(1, RoundingMode.UP).toDouble().toString()
            val maxNonLiftingMass = sharedViewModel.maxNonLiftingMass.value ?: 0.0
            if (it > maxNonLiftingMass) {
                binding.twMasseNTTeileErgebnis.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error_text_color)
                )
                binding.twMasseNTTeileErgebnis.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.error_background_color)
                )
            }
            else {
                binding.twMasseNTTeileErgebnis.setTextColor(defaultTextColor)
                binding.twMasseNTTeileErgebnis.setBackgroundResource(android.R.color.transparent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_bar_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miSettings -> findNavController().navigate(R.id.settingsFragment)
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}