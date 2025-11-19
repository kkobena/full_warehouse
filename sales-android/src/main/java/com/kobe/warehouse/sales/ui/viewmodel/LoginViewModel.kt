package com.kobe.warehouse.sales.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.sales.data.model.auth.Account
import com.kobe.warehouse.sales.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Login screen
 * Manages UI state and business logic for authentication
 * Following the same logic as web's LoginComponent and LoginService
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // LiveData for authentication state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData for authentication error
    private val _authenticationError = MutableLiveData<Boolean>()
    val authenticationError: LiveData<Boolean> = _authenticationError

    // LiveData for error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // LiveData for successful login (navigates to main screen)
    private val _loginSuccess = MutableLiveData<Account>()
    val loginSuccess: LiveData<Account> = _loginSuccess

    // LiveData for saved credentials (auto-fill)
    private val _savedCredentials = MutableLiveData<Pair<String, String>?>()
    val savedCredentials: LiveData<Pair<String, String>?> = _savedCredentials

    // LiveData for remember me checkbox
    private val _rememberMe = MutableLiveData<Boolean>()
    val rememberMe: LiveData<Boolean> = _rememberMe

    init {
        // Check for saved credentials on init
        checkSavedCredentials()
    }

    /**
     * Login with username and password
     * Same logic as web's LoginService.login() and LoginComponent.login()
     *
     * @param username Username
     * @param password Password
     * @param rememberMe Remember me flag
     */
    fun login(username: String, password: String, rememberMe: Boolean) {
        // Validate inputs
        if (username.isBlank() || password.isBlank()) {
            _errorMessage.value = "Veuillez saisir votre nom d'utilisateur et mot de passe"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _authenticationError.value = false
            _errorMessage.value = null

            val result = authRepository.login(username, password, rememberMe)

            result.fold(
                onSuccess = { account ->
                    _isLoading.value = false
                    _authenticationError.value = false
                    _loginSuccess.value = account
                },
                onFailure = { exception ->
                    _isLoading.value = false
                    _authenticationError.value = true
                    _errorMessage.value = exception.message ?: "Erreur de connexion"
                }
            )
        }
    }

    /**
     * Check for saved credentials (for auto-fill)
     */
    private fun checkSavedCredentials() {
        val credentials = authRepository.getSavedCredentials()
        _savedCredentials.value = credentials
        _rememberMe.value = credentials != null
    }

    /**
     * Auto-login if credentials are saved and user was already authenticated
     */
    fun autoLogin() {
        val credentials = authRepository.getSavedCredentials()
        if (credentials != null && authRepository.isAuthenticated()) {
            login(credentials.first, credentials.second, true)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _authenticationError.value = false
        _errorMessage.value = null
    }
}
