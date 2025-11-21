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

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun login(username: String, password: String, rememberMe: Boolean) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Veuillez remplir tous les champs")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            authRepository.login(username, password).fold(
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
}