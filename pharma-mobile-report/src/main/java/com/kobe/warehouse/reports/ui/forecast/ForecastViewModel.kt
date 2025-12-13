package com.kobe.warehouse.reports.ui.forecast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kobe.warehouse.reports.data.api.RetrofitClient
import com.kobe.warehouse.reports.data.local.AppDatabase
import com.kobe.warehouse.reports.data.model.ForecastData
import com.kobe.warehouse.reports.data.model.ForecastRequest
import com.kobe.warehouse.reports.data.offline.OfflineManager
import com.kobe.warehouse.reports.data.repository.ForecastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Forecast Screen
 * Manages forecast data and state
 */
class ForecastViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ForecastRepository

    private val _forecastState = MutableStateFlow<ForecastState>(ForecastState.Loading)
    val forecastState: StateFlow<ForecastState> = _forecastState.asStateFlow()

    init {
        val apiService = RetrofitClient.getInstance(application).create()
        val database = AppDatabase.getInstance(application)
        val offlineManager = OfflineManager.getInstance(application)

        repository = ForecastRepository(
            context = application,
            apiService = apiService,
            database = database,
            offlineManager = offlineManager
        )
    }

    /**
     * Load forecast data
     *
     * @param forceRefresh If true, force regeneration of forecast
     */
    fun loadForecast(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _forecastState.value = ForecastState.Loading

            val result = if (forceRefresh) {
                repository.refreshForecast()
            } else {
                repository.generateForecast()
            }

            _forecastState.value = result.fold(
                onSuccess = { ForecastState.Success(it) },
                onFailure = { ForecastState.Error(it.message ?: "Erreur lors de la génération des prévisions") }
            )
        }
    }

    /**
     * Load forecast with custom parameters
     */
    fun loadForecast(request: ForecastRequest) {
        viewModelScope.launch {
            _forecastState.value = ForecastState.Loading

            val result = repository.generateForecast(request)

            _forecastState.value = result.fold(
                onSuccess = { ForecastState.Success(it) },
                onFailure = { ForecastState.Error(it.message ?: "Erreur lors de la génération des prévisions") }
            )
        }
    }

    /**
     * Get confidence level description
     */
    fun getConfidenceDescription(confidence: Float): String {
        return repository.getConfidenceLevel(confidence)
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }

    /**
     * Forecast state sealed class
     */
    sealed class ForecastState {
        object Loading : ForecastState()
        data class Success(val data: ForecastData) : ForecastState()
        data class Error(val message: String) : ForecastState()
    }
}
