package com.danielschatz.gliderweightbalance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.danielschatz.gliderweightbalance.databinding.FragmentMainPagerBinding

class MainPagerFragment : Fragment() {

    private var _binding: FragmentMainPagerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var isFirstStart = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = true // Enable swiping between tabs
        binding.viewPager.reduceDragSensitivity(4) // Reduce horizontal swipe sensitivity

        // Link with Activity's BottomNavigationView
        (activity as? MainActivity)?.setupViewPagerWithBottomNav(binding.viewPager)

        // Nur beim echten App-Start (nicht beim Zurücknavigieren) den Tab erzwingen
        if (isFirstStart && savedInstanceState == null) {
            val lastId = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("last_selected_aircraft_id", -1)
            
            if (lastId != -1) {
                binding.viewPager.setCurrentItem(1, false) // Rechner (Home)
            }
            isFirstStart = false
        }
    }

    /**
     * Reduces the sensitivity of horizontal swipes to prevent accidental tab switching
     * while scrolling vertically in the calculator list.
     */
    private fun ViewPager2.reduceDragSensitivity(multiplier: Int) {
        try {
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(this) as RecyclerView

            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(recyclerView) as Int
            touchSlopField.set(recyclerView, touchSlop * multiplier)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun switchToHome() {
        binding.viewPager.currentItem = 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.setupViewPagerWithBottomNav(null)
        _binding = null
    }

    private class MainPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AircraftFragment()
                1 -> HomeFragment()
                else -> throw IllegalStateException("Unexpected position $position")
            }
        }
    }
}
