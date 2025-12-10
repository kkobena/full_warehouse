package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.*
import com.kobe.warehouse.reports.data.api.Account
import com.kobe.warehouse.reports.data.repository.ReportRepository
import com.kobe.warehouse.reports.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * ViewModel for Login screen.
 */
class LoginViewModel(
    private val repository: ReportRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // =========================================================================
    // LIVEDATA
    // =========================================================================

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _loginSuccess = MutableLiveData<Account>()
    val loginSuccess: LiveData<Account> = _loginSuccess

    // =========================================================================
    // LOGIN ACTIONS
    // =========================================================================

    /**
     * Perform login.
     */
    fun login(username: String, password: String, rememberMe: Boolean) {
        // Validate input
        if (username.isBlank() || password.isBlank()) {
            _errorMessage.value = "Veuillez remplir tous les champs"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.login(username, password, rememberMe)

            result.fold(
                onSuccess = { account ->
                    _loginSuccess.value = account
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message ?: "Échec de connexion"
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Check if auto-login is possible.
     */
    fun checkAutoLogin(): Boolean {
        return tokenManager.isAuthenticated()
    }

    /**
     * Get saved credentials if Remember Me is enabled.
     */
    fun getSavedCredentials(): Pair<String?, String?>? {
        return if (tokenManager.isRememberMeEnabled()) {
            Pair(tokenManager.getSavedUsername(), tokenManager.getSavedPassword())
        } else {
            null
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Factory for LoginViewModel.
 */
class LoginViewModelFactory(
    private val repository: ReportRepository,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
