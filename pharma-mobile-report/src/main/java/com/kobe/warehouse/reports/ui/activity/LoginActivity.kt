package com.kobe.warehouse.reports.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.databinding.ActivityLoginBinding
import com.kobe.warehouse.reports.ui.viewmodel.LoginViewModel
import com.kobe.warehouse.reports.ui.viewmodel.LoginViewModelFactory

/**
 * Login activity - entry point for the app.
 * Handles user authentication and auto-login.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if server URL is configured first (required for release builds)
        if (!isServerUrlConfigured()) {
            navigateToServerConfig()
            return
        }

        setupViewModel()
        checkAutoLogin()
        setupViews()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Re-check URL configuration after returning from ServerConfigActivity
        if (!::viewModel.isInitialized && isServerUrlConfigured()) {
            setupViewModel()
            checkAutoLogin()
            setupViews()
            observeViewModel()
        }
    }

    /**
     * Check if server URL is configured.
     */
    private fun isServerUrlConfigured(): Boolean {
        val tokenManager = PharmaReportApplication.getTokenManager()
        val serverUrl = tokenManager.getServerUrl()
        return serverUrl.isNotBlank()
    }

    /**
     * Setup ViewModel with dependencies.
     */
    private fun setupViewModel() {
        val tokenManager = PharmaReportApplication.getTokenManager()
        val repository = PharmaReportApplication.getRepository()

        val factory = LoginViewModelFactory(repository, tokenManager)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]
    }

    /**
     * Check if user can auto-login with saved credentials.
     */
    private fun checkAutoLogin() {
        // If already authenticated, go directly to dashboard
        if (viewModel.checkAutoLogin()) {
            navigateToDashboard()
            return
        }

        // If Remember Me enabled, pre-fill credentials
        viewModel.getSavedCredentials()?.let { (username, password) ->
            binding.etUsername.setText(username)
            binding.etPassword.setText(password)
            binding.cbRememberMe.isChecked = true
        }
    }

    /**
     * Setup view listeners.
     */
    private fun setupViews() {
        // Login button click
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        // Password field IME action
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin()
                true
            } else {
                false
            }
        }

        // Server config button
        binding.btnServerConfig.setOnClickListener {
            navigateToServerConfig()
        }

        // Clear error when typing
        binding.etUsername.setOnFocusChangeListener { _, _ ->
            viewModel.clearError()
        }
        binding.etPassword.setOnFocusChangeListener { _, _ ->
            viewModel.clearError()
        }
    }

    /**
     * Observe ViewModel LiveData.
     */
    private fun observeViewModel() {
        // Loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.btnLogin.isEnabled = !isLoading
            binding.etUsername.isEnabled = !isLoading
            binding.etPassword.isEnabled = !isLoading
        }

        // Error message
        viewModel.errorMessage.observe(this) { error ->
            binding.tvError.isVisible = error != null
            binding.tvError.text = error
        }

        // Login success
        viewModel.loginSuccess.observe(this) { account ->
            navigateToDashboard()
        }
    }

    /**
     * Perform login with entered credentials.
     */
    private fun performLogin() {
        val username = binding.etUsername.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        val rememberMe = binding.cbRememberMe.isChecked

        viewModel.login(username, password, rememberMe)
    }

    /**
     * Navigate to dashboard screen.
     */
    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * Navigate to server configuration screen.
     */
    private fun navigateToServerConfig() {
        val intent = Intent(this, ServerConfigActivity::class.java)
        startActivity(intent)
    }
}
