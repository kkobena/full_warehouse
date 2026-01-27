package com.kobe.warehouse.sales.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.SalesApiService
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.databinding.ActivityFullsaleHomeBinding
import com.kobe.warehouse.sales.ui.fragment.PreventeFragment
import com.kobe.warehouse.sales.ui.fragment.VenteEnCoursFragment
import com.kobe.warehouse.sales.ui.viewmodel.FullSaleHomeViewModel
import com.kobe.warehouse.sales.ui.viewmodel.FullSaleHomeViewModelFactory
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FullSaleHomeActivity
 * Main sales home screen with tabs for ongoing sales and preventes
 * Extends BaseActivity for automatic session management
 *
 * Features:
 * - TabLayout with 2 tabs: "Ventes en cours" and "Préventes"
 * - ViewPager2 for fragment navigation
 * - Search field with debounce
 * - FAB button for new sale
 * - Pull-to-refresh in fragments
 */
class FullSaleHomeActivity : BaseActivity() {

    private lateinit var binding: ActivityFullsaleHomeBinding
    private lateinit var viewModel: FullSaleHomeViewModel
    private lateinit var viewPagerAdapter: SalesPagerAdapter

    private var searchJob: Job? = null
    private val searchDebounceTime = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFullsaleHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupToolbar()
        setupViewPager()
        setupTabs()
        setupSearch()
        setupListeners()
    }

    /**
     * Setup ViewModel with dependencies
     */
    private fun setupViewModel() {
        val tokenManager = TokenManager(this)
        val retrofit = ApiClient.create(tokenManager = tokenManager)
        val salesApiService = retrofit.create(SalesApiService::class.java)
        val salesRepository = SalesRepository(salesApiService)

        val factory = FullSaleHomeViewModelFactory(salesRepository)
        viewModel = ViewModelProvider(this, factory)[FullSaleHomeViewModel::class.java]
    }

    /**
     * Setup toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            title = "Ventes"
        }
    }

    /**
     * Setup ViewPager2 with fragments
     */
    private fun setupViewPager() {
        viewPagerAdapter = SalesPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        // Disable swipe if needed (optional)
        // binding.viewPager.isUserInputEnabled = false
    }

    /**
     * Setup TabLayout with ViewPager2
     */
    private fun setupTabs() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Ventes en cours"
                1 -> "Préventes"
                else -> ""
            }
        }.attach()
    }

    /**
     * Setup search field with debounce
     */
    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            // Cancel previous search job
            searchJob?.cancel()

            // Create new search job with debounce
            searchJob = CoroutineScope(Dispatchers.Main).launch {
                delay(searchDebounceTime)
                performSearch(text.toString())
            }
        }
    }

    /**
     * Perform search based on current tab
     */
    private fun performSearch(query: String) {
        val currentTab = binding.viewPager.currentItem
        when (currentTab) {
            0 -> viewModel.searchOngoingSales(query)
            1 -> viewModel.searchPreventes(query)
        }
    }

    /**
     * Setup listeners
     */
    private fun setupListeners() {
        // FAB new sale button
        binding.fabNewSale.setOnClickListener {
            createNewSale()
        }

        // Listen to tab changes to update FAB text
        binding.tabLayout.addOnTabSelectedListener(
            object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                    updateFabText(tab?.position ?: 0)
                }

                override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
                override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            }
        )
    }

    /**
     * Update FAB text based on selected tab
     */
    private fun updateFabText(tabPosition: Int) {
        binding.fabNewSale.text = when (tabPosition) {
            0 -> "Nouvelle Vente"
            1 -> "Nouvelle Prévente"
            else -> "Nouvelle Vente"
        }
    }

    /**
     * Create new sale
     */
    private fun createNewSale() {
        val intent = Intent(this, ComptantSaleActivity::class.java)
        startActivity(intent)
    }

    /**
     * Refresh when returning from sale activity
     */
    override fun onResume() {
        super.onResume()
        // Refresh current tab data
        val currentTab = binding.viewPager.currentItem
        when (currentTab) {
            0 -> viewModel.refreshOngoingSales()
            1 -> viewModel.refreshPreventes()
        }
    }

    /**
     * ViewPager2 Adapter for sales fragments
     */
    private inner class SalesPagerAdapter(activity: FragmentActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> VenteEnCoursFragment.newInstance()
                1 -> PreventeFragment.newInstance()
                else -> VenteEnCoursFragment.newInstance()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
    }
}
