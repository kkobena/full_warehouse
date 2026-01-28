package com.kobe.warehouse.sales.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.ui.viewmodel.CustomerSelectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for CustomerSelectionViewModel
 *
 * Tests:
 * - Customer search
 * - Customer selection
 * - Ayants droit loading (currently placeholder in ViewModel)
 * - Eligibility checks (Assurance, Carnet - currently placeholders)
 * - Available credit calculation
 */
@ExperimentalCoroutinesApi
class CustomerSelectionViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var customerRepository: CustomerRepository

    private lateinit var viewModel: CustomerSelectionViewModel

    // Test data
    private val testCustomer = Customer(
        id = 1L,
        firstName = "Jean",
        lastName = "Dupont",
        phone = "0123456789",
        email = "jean.dupont@example.com"
    )

    private val testCustomer2 = Customer(
        id = 2L,
        firstName = "Marie",
        lastName = "Martin",
        phone = "0987654321",
        email = "marie.martin@example.com"
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = CustomerSelectionViewModel(customerRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== Customer Search Tests =====

    @Test
    fun `searchCustomers with valid query should return results`() = runTest {
        // Given
        val customers = listOf(testCustomer, testCustomer2)
        whenever(customerRepository.searchCustomers("Dupont")).thenReturn(Result.success(customers))

        // When
        viewModel.searchCustomers("Dupont")
        advanceUntilIdle()

        // Then
        assertEquals(customers, viewModel.customers.value)
        verify(customerRepository).searchCustomers("Dupont")
    }

    @Test
    fun `searchCustomers with short query should not search`() = runTest {
        // When
        viewModel.searchCustomers("D")
        advanceUntilIdle()

        // Then
        assertEquals(emptyList(), viewModel.customers.value)
    }

    @Test
    fun `searchCustomers with error should set error message`() = runTest {
        // Given
        val error = Exception("Network error")
        // Use concrete value instead of any() to avoid matcher issues
        whenever(customerRepository.searchCustomers("Dupont")).thenReturn(Result.failure(error))

        // When
        viewModel.searchCustomers("Dupont")
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Erreur"))
    }

    // ===== Customer Selection Tests =====

    @Test
    fun `selectCustomer should update selected customer`() {
        // When
        viewModel.selectCustomer(testCustomer)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(testCustomer, viewModel.selectedCustomer.value)
    }

    @Test
    fun `clearSelectedCustomer should clear customer`() {
        // Given
        viewModel.selectCustomer(testCustomer)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearSelectedCustomer()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.selectedCustomer.value)
    }

    // ===== Ayants Droit Tests =====

    @Test
    fun `loadAyantsDroit should update ayantsDroit LiveData`() = runTest {
        // When - Currently returns empty list (TODO in ViewModel)
        viewModel.loadAyantsDroit(testCustomer.id)
        advanceUntilIdle()

        // Then
        assertEquals(emptyList(), viewModel.ayantsDroit.value)
    }

    // ===== Eligibility Tests =====

    @Test
    fun `isEligibleForAssurance should return true for customer`() {
        // When - Currently returns true (placeholder in ViewModel)
        val result = viewModel.isEligibleForAssurance(testCustomer)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isEligibleForCarnet should return true for customer`() {
        // When - Currently returns true (placeholder in ViewModel)
        val result = viewModel.isEligibleForCarnet(testCustomer)

        // Then
        assertTrue(result)
    }

    // ===== Available Credit Tests =====

    @Test
    fun `getAvailableCredit should calculate from limit and balance`() {
        // When - Currently returns 0 (placeholders in ViewModel)
        val result = viewModel.getAvailableCredit(testCustomer)

        // Then - 0 - 0 = 0
        assertEquals(0, result)
    }

    @Test
    fun `getCustomerCreditLimit should return credit limit`() {
        // When - Currently returns 0 (placeholder in ViewModel)
        val result = viewModel.getCustomerCreditLimit(testCustomer)

        // Then
        assertEquals(0, result)
    }

    @Test
    fun `getCustomerCarnetBalance should return balance`() {
        // When - Currently returns 0 (placeholder in ViewModel)
        val result = viewModel.getCustomerCarnetBalance(testCustomer)

        // Then
        assertEquals(0, result)
    }

    // ===== Error Handling Tests =====

    @Test
    fun `clearError should clear error message`() {
        // Given - Trigger an error by searching with mock that fails
        viewModel.searchCustomers("test")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.errorMessage.value)
    }
}
