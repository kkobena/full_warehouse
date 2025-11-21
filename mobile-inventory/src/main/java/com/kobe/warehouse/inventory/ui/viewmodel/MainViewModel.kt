package com.kobe.warehouse.inventory.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.inventory.data.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class MainState {
    object Idle : MainState()
    object Loading : MainState()
    data class Success(val username: String) : MainState()
    object LoggedOut : MainState()
    data class Error(val message: String) : MainState()
}

class MainViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _mainState = MutableLiveData<MainState>(MainState.Idle)
    val mainState: LiveData<MainState> = _mainState

    fun loadCurrentUser() {
        viewModelScope.launch {
            _mainState.value = MainState.Loading
            authRepository.getAccount().fold(
                onSuccess = { account ->
                    _mainState.value = MainState.Success(account.login)
                },
                onFailure = { error ->
                    _mainState.value = MainState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _mainState.value = MainState.Loading
            authRepository.logout().fold(
                onSuccess = {
                    _mainState.value = MainState.LoggedOut
                },
                onFailure = {
                    // Even if logout fails, we want to go back to the login screen
                    _mainState.value = MainState.LoggedOut
                }
            )
        }
    }
}
