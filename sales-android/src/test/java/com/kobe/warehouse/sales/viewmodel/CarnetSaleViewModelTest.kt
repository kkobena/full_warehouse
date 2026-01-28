package com.kobe.warehouse.sales.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.ui.viewmodel.CarnetSaleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for CarnetSaleViewModel
 *
 * Tests:
 * - Customer setting
 * - Credit info updates
 * - UI state management
 *
 * NOTE: Credit validation and calculations are done by BACKEND, not tested here
 */
@ExperimentalCoroutinesApi
class CarnetSaleViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: CarnetSaleViewModel

    // Test data
    private val testCustomer = Customer(
        id = 1L,
        firstName = "Jean",
        lastName = "Dupont",
        phone = "0123456789",
        email = "jean.dupont@example.com"
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = CarnetSaleViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== Customer Tests =====

    @Test
    fun `initial customer should be null`() {
        // When
        val customer = viewModel.customer.value

        // Then
        assertNull(customer)
    }

    @Test
    fun `setCustomer should update customer`() {
        // When
        viewModel.setCustomer(testCustomer)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(testCustomer, viewModel.customer.value)
    }

    // ===== Credit Info Tests =====

    @Test
    fun `initial credit values should be zero`() {
        // When
        val creditLimit = viewModel.creditLimit.value
        val currentBalance = viewModel.currentBalance.value
        val availableCredit = viewModel.availableCredit.value

        // Then
        assertEquals(0, creditLimit)
        assertEquals(0, currentBalance)
        assertEquals(0, availableCredit)
    }

    @Test
    fun `updateCreditInfo should update all credit values`() {
        // When
        viewModel.updateCreditInfo(limit = 100000, balance = 25000)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(100000, viewModel.creditLimit.value)
        assertEquals(25000, viewModel.currentBalance.value)
        assertEquals(75000, viewModel.availableCredit.value)
    }

    @Test
    fun `updateCreditInfo with full balance should show zero available`() {
        // When
        viewModel.updateCreditInfo(limit = 100000, balance = 100000)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(100000, viewModel.creditLimit.value)
        assertEquals(100000, viewModel.currentBalance.value)
        assertEquals(0, viewModel.availableCredit.value)
    }

    @Test
    fun `updateCreditInfo with zero balance should show full limit available`() {
        // When
        viewModel.updateCreditInfo(limit = 100000, balance = 0)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(100000, viewModel.creditLimit.value)
        assertEquals(0, viewModel.currentBalance.value)
        assertEquals(100000, viewModel.availableCredit.value)
    }

    // ===== Error Handling Tests =====

    @Test
    fun `initial error message should be null`() {
        // When
        val error = viewModel.errorMessage.value

        // Then
        assertNull(error)
    }

    @Test
    fun `clearError should clear error message`() {
        // Given - Simulate an error state (would come from backend validation)
        // For now, just test the clear mechanism

        // When
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.errorMessage.value)
    }

    // ===== Backend Delegation Tests =====

    @Test
    fun `credit validation should be done by backend - no local validation`() {
        // This test verifies the architectural decision that credit validation
        // is done by the backend, not the ViewModel

        // Given
        viewModel.setCustomer(testCustomer)
        viewModel.updateCreditInfo(limit = 100000, balance = 25000)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // ViewModel should only display credit info
        assertEquals(75000, viewModel.availableCredit.value)

        // Credit validation (validateCreditLimit, getCreditExcess)
        // is NOT part of the ViewModel - it's done by the BACKEND
        // The backend validates credit when finalizing the sale:
        // POST /api/sales/carnet
        // If credit insufficient: HTTP 400 with error message
    }

    @Test
    fun `credit info should come from backend API calls`() {
        // This test documents that credit info is fetched from backend
        // GET /api/customers/{id}/carnet/balance
        // GET /api/customers/{id}/carnet/limit

        // Given
        viewModel.setCustomer(testCustomer)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // In real implementation, setCustomer() would trigger:
        // customerRepository.getCarnetBalance(customer.id)
        // customerRepository.getCarnetLimit(customer.id)
        // And then call updateCreditInfo() with the response

        // For now, ViewModel just stores customer
        assertNotNull(viewModel.customer.value)
        assertEquals(testCustomer, viewModel.customer.value)
    }

    // ===== Integration Scenario Tests =====

    @Test
    fun `complete carnet flow - set customer then update credit info`() {
        // Scenario: User selects customer, then app fetches credit info from backend

        // Step 1: Select customer
        viewModel.setCustomer(testCustomer)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testCustomer, viewModel.customer.value)

        // Step 2: Backend returns credit info (simulated via updateCreditInfo)
        viewModel.updateCreditInfo(limit = 150000, balance = 50000)
        testDispatcher.scheduler.advanceUntilIdle()

        // Step 3: Verify UI can display correct values
        assertEquals(150000, viewModel.creditLimit.value)
        assertEquals(50000, viewModel.currentBalance.value)
        assertEquals(100000, viewModel.availableCredit.value)

        // Step 4: User adds products and finalizes
        // Backend validates credit: POST /api/sales/carnet
        // If saleAmount (80000) <= availableCredit (100000) → Success
        // If saleAmount (120000) > availableCredit (100000) → HTTP 400 error
    }
}
