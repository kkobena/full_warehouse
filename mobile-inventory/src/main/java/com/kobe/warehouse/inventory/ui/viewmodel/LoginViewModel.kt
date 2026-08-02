package com.kobe.warehouse.inventory.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.inventory.data.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val username: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

/**
 * ViewModel de l'écran de connexion.
 *
 * Aligné sur `sales-android/ui/viewmodel/LoginViewModel` : mémorisation des
 * identifiants, pré-remplissage et connexion automatique.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    /** Identifiants mémorisés, pour pré-remplir le formulaire */
    private val _savedCredentials = MutableLiveData<Pair<String, String>?>()
    val savedCredentials: LiveData<Pair<String, String>?> = _savedCredentials

    private val _rememberMe = MutableLiveData<Boolean>()
    val rememberMe: LiveData<Boolean> = _rememberMe

    init {
        checkSavedCredentials()
    }

    fun login(username: String, password: String, rememberMe: Boolean) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error(
                "Veuillez saisir votre nom d'utilisateur et mot de passe"
            )
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            authRepository.login(username.trim(), password, rememberMe).fold(
                onSuccess = {
                    _loginState.value = LoginState.Success(username)
                },
                onFailure = { error ->
                    _loginState.value = LoginState.Error(
                        error.message ?: "Erreur de connexion"
                    )
                }
            )
        }
    }

    private fun checkSavedCredentials() {
        val credentials = authRepository.getSavedCredentials()
        _savedCredentials.value = credentials
        _rememberMe.value = credentials != null
    }

    /**
     * Connexion automatique si des identifiants sont mémorisés et que la session
     * précédente est encore valide.
     */
    fun autoLogin() {
        val credentials = authRepository.getSavedCredentials()
        if (credentials != null && authRepository.isAuthenticated()) {
            login(credentials.first, credentials.second, true)
        }
    }

    fun clearError() {
        if (_loginState.value is LoginState.Error) {
            _loginState.value = LoginState.Idle
        }
    }
}
