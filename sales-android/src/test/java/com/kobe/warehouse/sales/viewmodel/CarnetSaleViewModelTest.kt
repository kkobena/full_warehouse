package com.kobe.warehouse.sales.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.domain.model.CarnetData
import com.kobe.warehouse.sales.ui.viewmodel.CarnetSaleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for CarnetSaleViewModel
 *
 * Tests:
 * - Carnet data loading
 * - Credit limit validation
 * - Sale amount validation
 * - Credit usage warnings
 */
@ExperimentalCoroutinesApi
class CarnetSaleViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var customerRepository: CustomerRepository

    private lateinit var viewModel: CarnetSaleViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = CarnetSaleViewModel(customerRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Carnet Data Tests ==========

    @Test
    fun `loadCarnetData should set carnetData on success`() = runTest {
        val customer = Customer(id = 1, firstName = "John", lastName = "Doe")
        val carnetData = CarnetData.create(
            customer = customer,
            limiteCredit = 100000,
            encours = 25000
        )
        whenever(customerRepository.getCustomerCarnetData(customer.id)).thenReturn(Result.success(carnetData))

        viewModel.loadCarnetData(customer)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.carnetData.value
        assertNotNull(result)
        assertEquals(100000, result.limiteCredit)
        assertEquals(25000, result.encours)
        assertEquals(75000, result.creditDisponible)
    }

    @Test
    fun `loadCarnetData should update error on failure`() = runTest {
        val customer = Customer(id = 1, firstName = "John", lastName = "Doe")
        whenever(customerRepository.getCustomerCarnetData(customer.id)).thenReturn(
            Result.failure(Exception("Network error"))
        )

        viewModel.loadCarnetData(customer)
        testDispatcher.scheduler.advanceUntilIdle()

        val error = viewModel.errorMessage.value
        assertNotNull(error)
        assertTrue(error.contains("Erreur"))
    }

    // ========== Validation Tests ==========

    @Test
    fun `validateSaleAmount should succeed when within credit limit`() = runTest {
        val customer = Customer(id = 1, firstName = "John", lastName = "Doe")
        val carnetData = CarnetData.create(
            customer = customer,
            limiteCredit = 100000,
            encours = 25000
        )
        whenever(customerRepository.getCustomerCarnetData(customer.id)).thenReturn(Result.success(carnetData))

        viewModel.loadCarnetData(customer)
        testDispatcher.scheduler.advanceUntilIdle()

        val isValid = viewModel.validateSaleAmount(50000)

        assertTrue(isValid)
        assertEquals(true, viewModel.canFinalizeSale.value)
    }

    @Test
    fun `validateSaleAmount should fail when exceeds credit limit`() = runTest {
        val customer = Customer(id = 1, firstName = "John", lastName = "Doe")
        val carnetData = CarnetData.create(
            customer = customer,
            limiteCredit = 100000,
            encours = 25000
        )
        whenever(customerRepository.getCustomerCarnetData(customer.id)).thenReturn(Result.success(carnetData))

        viewModel.loadCarnetData(customer)
        testDispatcher.scheduler.advanceUntilIdle()

        val isValid = viewModel.validateSaleAmount(80000) // Would exceed limit

        assertEquals(false, isValid)
        assertEquals(false, viewModel.canFinalizeSale.value)
        val error = viewModel.errorMessage.value
        assertNotNull(error)
        assertTrue(error.contains("Limite de crédit"))
    }

    @Test
    fun `validateSaleAmount should show warning when usage will be high`() = runTest {
        val customer = Customer(id = 1, firstName = "John", lastName = "Doe")
        val carnetData = CarnetData.create(
            customer = customer,
            limiteCredit = 100000,
            encours = 25000
        )
        whenever(customerRepository.getCustomerCarnetData(customer.id)).thenReturn(Result.success(carnetData))

        viewModel.loadCarnetData(customer)
        testDispatcher.scheduler.advanceUntilIdle()

        val isValid = viewModel.validateSaleAmount(65000) // Will result in 90% usage

        assertTrue(isValid)
        val warning = viewModel.warningMessage.value
        assertNotNull(warning)
        assertTrue(warning.contains("90%"))
    }

    @Test
    fun `validateSaleAmount should fail without carnet data`() {
        val isValid = viewModel.validateSaleAmount(10000)

        assertEquals(false, isValid)
        val error = viewModel.errorMessage.value
        assertNotNull(error)
        assertTrue(error.contains("Données carnet"))
    }

    // ========== Credit Calculation Tests ==========

    @Test
    fun `getMaxSaleAmount should return available credit`() = runTest {
        val customer = Customer(id = 1, firstName = "John", lastName = "Doe")
        val carnetData = CarnetData.create(
            customer = customer,
            limiteCredit = 100000,
            encours = 25000
        )
        whenever(customerRepository.getCustomerCarnetData(customer.id)).thenReturn(Result.success(carnetData))

        viewModel.loadCarnetData(customer)
        testDispatcher.scheduler.advanceUntilIdle()

        val maxAmount = viewModel.getMaxSaleAmount()

        assertEquals(75000, maxAmount)
    }

    @Test
    fun `calculateNewCreditState should calculate correct values`() = runTest {
        val customer = Customer(id = 1, firstName = "John", lastName = "Doe")
        val carnetData = CarnetData.create(
            customer = customer,
            limiteCredit = 100000,
            encours = 25000
        )
        whenever(customerRepository.getCustomerCarnetData(customer.id)).thenReturn(Result.success(carnetData))

        viewModel.loadCarnetData(customer)
        testDispatcher.scheduler.advanceUntilIdle()

        val (newEncours, newCreditDispo, newUsage) = viewModel.calculateNewCreditState(30000)!!

        assertEquals(55000, newEncours)
        assertEquals(45000, newCreditDispo)
        assertEquals(55, newUsage)
    }

    @Test
    fun `calculateNewCreditState should return null without carnet data`() {
        val result = viewModel.calculateNewCreditState(10000)

        assertEquals(null, result)
    }

    // ========== Reset Tests ==========

    @Test
    fun `reset should clear all data`() = runTest {
        val customer = Customer(id = 1, firstName = "John", lastName = "Doe")
        val carnetData = CarnetData.create(
            customer = customer,
            limiteCredit = 100000,
            encours = 25000
        )
        whenever(customerRepository.getCustomerCarnetData(customer.id)).thenReturn(Result.success(carnetData))

        viewModel.loadCarnetData(customer)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.reset()

        assertEquals(null, viewModel.carnetData.value)
        assertEquals(false, viewModel.canFinalizeSale.value)
        assertEquals(null, viewModel.errorMessage.value)
        assertEquals(null, viewModel.warningMessage.value)
    }
}
