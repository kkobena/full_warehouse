package com.kobe.warehouse.inventory.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.data.repository.AuthRepository
import com.kobe.warehouse.inventory.databinding.ActivityMainBinding
import com.kobe.warehouse.inventory.ui.viewmodel.MainState
import com.kobe.warehouse.inventory.ui.viewmodel.MainViewModel
import com.kobe.warehouse.inventory.utils.TokenManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(AuthRepository(TokenManager(this)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupObservers()

        mainViewModel.loadCurrentUser()
    }

    private fun setupListeners() {
        binding.btnInventories.setOnClickListener {
            val intent = Intent(this, InventoryListActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            mainViewModel.logout()
        }
    }

    private fun setupObservers() {
        mainViewModel.mainState.observe(this) { state ->
            when (state) {
                is MainState.Idle -> {
                    // Do nothing
                }
                is MainState.Loading -> {
                    // Show loading indicator if needed
                }
                is MainState.Success -> {
                    binding.tvWelcome.text = getString(R.string.welcome, state.username)
                }
                is MainState.LoggedOut -> {
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                is MainState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ViewModelFactory for MainViewModel
    class MainViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}