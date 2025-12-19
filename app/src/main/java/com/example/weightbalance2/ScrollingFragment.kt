package com.example.weightbalance2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels

class ScrollingFragment : Fragment() {

    private val sharedViewModel: SharedViewModel by navGraphViewModels(R.id.main_nav)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scrolling, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val input = view.findViewById<EditText>(R.id.etPilot)

        input.addTextChangedListener {
            sharedViewModel.pilotMass.value =
                it.toString().toDoubleOrNull()
        }
    }

}