package com.kobe.warehouse.sales.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kobe.warehouse.sales.ui.dialog.AssureStep1Fragment
import com.kobe.warehouse.sales.ui.dialog.AssureStep2Fragment

/**
 * ViewPager2 Adapter for Assure Customer Creation Steps
 */
class AssureFormStepAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2  // 2 steps

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AssureStep1Fragment()
            1 -> AssureStep2Fragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
