package com.kobe.warehouse.reports.ui.viewmodel

import androidx.lifecycle.*
import com.kobe.warehouse.reports.data.model.TodoItem
import com.kobe.warehouse.reports.data.model.TodoList
import com.kobe.warehouse.reports.data.repository.ReportRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Todos screen.
 */
class TodosViewModel(
    private val repository: ReportRepository
) : ViewModel() {

    // =========================================================================
    // LIVEDATA
    // =========================================================================

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _todoList = MutableLiveData<TodoList>()
    val todoList: LiveData<TodoList> = _todoList

    val allItems: LiveData<List<TodoItem>> = _todoList.map { it.getAllItems() }

    val urgentCount: LiveData<Int> = _todoList.map { it.urgent.size }
    val importantCount: LiveData<Int> = _todoList.map { it.important.size }
    val normalCount: LiveData<Int> = _todoList.map { it.normal.size }

    val isEmpty: LiveData<Boolean> = _todoList.map { it.isEmpty() }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    /**
     * Load todos from server.
     */
    fun loadTodos() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getTodos()

            result.fold(
                onSuccess = { todos ->
                    _todoList.value = todos
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isLoading.value = false
        }
    }

    /**
     * Refresh todos (pull-to-refresh).
     */
    fun refreshTodos() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            val result = repository.getTodos()

            result.fold(
                onSuccess = { todos ->
                    _todoList.value = todos
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message
                }
            )

            _isRefreshing.value = false
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
 * Factory for TodosViewModel.
 */
class TodosViewModelFactory(
    private val repository: ReportRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodosViewModel::class.java)) {
            return TodosViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
