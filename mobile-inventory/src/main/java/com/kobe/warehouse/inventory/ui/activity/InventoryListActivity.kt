package com.kobe.warehouse.inventory.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.inventory.data.repository.InventoryRepository
import com.kobe.warehouse.inventory.databinding.ActivityInventoryListBinding
import com.kobe.warehouse.inventory.ui.adapter.InventoryAdapter
import com.kobe.warehouse.inventory.ui.viewmodel.InventoryListState
import com.kobe.warehouse.inventory.ui.viewmodel.InventoryListViewModel
import com.kobe.warehouse.inventory.utils.TokenManager

class InventoryListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInventoryListBinding
    private val inventoryListViewModel: InventoryListViewModel by viewModels {
        InventoryListViewModelFactory(InventoryRepository(TokenManager(this)))
    }
    private lateinit var inventoryAdapter: InventoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        setupObservers()

        // Load inventories
        inventoryListViewModel.loadActiveInventories()
    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter { inventory ->
            // Navigate to InventoryDetailActivity
            val intent = Intent(this, InventoryDetailActivity::class.java)
            intent.putExtra(EXTRA_INVENTORY_ID, inventory.id)
            intent.putExtra(EXTRA_INVENTORY_NAME, inventory.name)
            startActivity(intent)
        }
        binding.rvInventories.adapter = inventoryAdapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            inventoryListViewModel.refreshInventories()
        }
    }

    private fun setupObservers() {
        inventoryListViewModel.inventoryListState.observe(this) { state ->
            when (state) {
                is InventoryListState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
                is InventoryListState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
                is InventoryListState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    if (state.inventories.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvInventories.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvInventories.visibility = View.VISIBLE
                        inventoryAdapter.submitList(state.inventories)
                    }
                }
                is InventoryListState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ViewModelFactory for InventoryListViewModel
    class InventoryListViewModelFactory(
        private val inventoryRepository: InventoryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventoryListViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return InventoryListViewModel(inventoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        const val EXTRA_INVENTORY_ID = "extra_inventory_id"
        const val EXTRA_INVENTORY_NAME = "extra_inventory_name"
    }
}
