package com.example.weightbalance2

import android.os.Bundle
import android.view.*
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



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        navController = Navigation.findNavController(view)

        sharedViewModel.totalMass.observe(viewLifecycleOwner) {
            binding.twGesamtgewichtOutput.text =
                it.toBigDecimal().setScale(1, RoundingMode.UP).toDouble().toString()
        }
        sharedViewModel.cg.observe(viewLifecycleOwner) {
            binding.twSchwerpunktlageErgebnis.text =
                it.toBigDecimal().setScale(1, RoundingMode.UP).toDouble().toString()
        }
        sharedViewModel.nonLiftingMass.observe(viewLifecycleOwner) {
            binding.twMasseNTTeileErgebnis.text =
                it.toBigDecimal().setScale(1, RoundingMode.UP).toDouble().toString()
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
        _binding = null
        super.onDestroyView()
    }
}