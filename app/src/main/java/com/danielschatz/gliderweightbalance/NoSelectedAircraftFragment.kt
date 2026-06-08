package com.danielschatz.gliderweightbalance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class NoAircraftSelectedFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Lade einfach das Layout. Mehr muss dieses Fragment nicht tun.
        return inflater.inflate(R.layout.fragment_no_selected_aircraft, container, false)
    }
}
    