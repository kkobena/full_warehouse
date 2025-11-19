package com.kobe.warehouse.sales.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.AuthApiService
import com.kobe.warehouse.sales.data.model.ServerConfig
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.databinding.ActivityLoginBinding
import com.kobe.warehouse.sales.ui.dialog.ServerConfigDialog
import com.kobe.warehouse.sales.ui.viewmodel.LoginViewModel
import com.kobe.warehouse.sales.ui.viewmodel.LoginViewModelFactory
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * Login Activity
 * Implements the same login UI and logic as the web application
 *
 * Features:
 * - Username/Password login
 * - Remember me checkbox
 * - Auto-fill saved credentials
 * - Auto-login if already authenticated
 * - Error handling with user feedback
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup ViewModel
        setupViewModel()

        // Setup UI
        setupUI()

        // Observe ViewModel
        observeViewModel()

        // Auto-login if authenticated
        viewModel.autoLogin()
    }

    /**
     * Setup ViewModel with dependencies
     */
    private fun setupViewModel() {
        val tokenManager = TokenManager(this)
        val retrofit = ApiClient.create(tokenManager = tokenManager)
        val authApiService = retrofit.create(AuthApiService::class.java)
        val authRepository = AuthRepository(authApiService, tokenManager)

        val factory = LoginViewModelFactory(authRepository)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]
    }

    /**
     * Setup UI components
     */
    private fun setupUI() {
        // Login button click listener
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            val rememberMe = binding.cbRememberMe.isChecked

            viewModel.login(username, password, rememberMe)
        }

        // Settings button click listener (server configuration)
        binding.btnSettings.setOnClickListener {
            showServerConfigDialog()
        }

        // Focus username field on start
        binding.etUsername.requestFocus()
    }

    /**
     * Show server configuration dialog
     */
    private fun showServerConfigDialog() {
        val tokenManager = TokenManager(this)
        val currentConfig = tokenManager.getServerConfig()

        val dialog = ServerConfigDialog.newInstance(currentConfig) { newConfig ->
            // Save the new configuration
            tokenManager.saveServerConfig(newConfig)

            // Show confirmation toast
            Toast.makeText(
                this,
                "Configuration sauvegardée: ${newConfig.getBaseUrl()}",
                Toast.LENGTH_SHORT
            ).show()

            // Recreate the activity to apply new server URL
            recreate()
        }

        dialog.show(supportFragmentManager, "ServerConfigDialog")
    }

    /**
     * Observe ViewModel LiveData
     */
    private fun observeViewModel() {
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showLoading()
            } else {
                hideLoading()
            }
        }

        // Observe authentication error
        viewModel.authenticationError.observe(this) { hasError ->
            if (hasError) {
                binding.tvErrorMessage.visibility = View.VISIBLE
            } else {
                binding.tvErrorMessage.visibility = View.GONE
            }
        }

        // Observe error message
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        // Observe login success
        viewModel.loginSuccess.observe(this) { account ->
            // Navigate directly to SalesHomeActivity (skip welcome screen)
            val intent = Intent(this, SalesHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Observe saved credentials for auto-fill
        viewModel.savedCredentials.observe(this) { credentials ->
            credentials?.let {
                binding.etUsername.setText(it.first)
                binding.etPassword.setText(it.second)
                binding.cbRememberMe.isChecked = true
            }
        }

        // Observe remember me checkbox
        viewModel.rememberMe.observe(this) { rememberMe ->
            binding.cbRememberMe.isChecked = rememberMe
        }
    }

    /**
     * Show loading state
     */
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = getString(R.string.connecting)
    }

    /**
     * Hide loading state
     */
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = getString(R.string.login)
    }
}

/**
 * ViewModelFactory for LoginViewModel
 */
class LoginViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
