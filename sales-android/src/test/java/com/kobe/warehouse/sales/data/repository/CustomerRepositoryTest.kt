package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.CreateCustomerRequest
import com.kobe.warehouse.sales.data.api.CustomerApiService
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.domain.model.CarnetData
import com.kobe.warehouse.sales.domain.model.InsuranceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for CustomerRepository
 * Tests customer search, creation, and data retrieval operations
 */
@ExperimentalCoroutinesApi
class CustomerRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var customerApiService: CustomerApiService

    private lateinit var repository: CustomerRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        repository = CustomerRepository(customerApiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== searchCustomers() Tests =====

    @Test
    fun `searchCustomers should return success with customer list`() = runTest {
        // Given
        val customers = listOf(
            Customer(id = 1L, firstName = "Jean", lastName = "Dupont", type = "STANDARD"),
            Customer(id = 2L, firstName = "Marie", lastName = "Durand", type = "ASSURE")
        )
        whenever(customerApiService.searchCustomers(eq("Dupont"), eq(20)))
            .thenReturn(Response.success(customers))

        // When
        val result = repository.searchCustomers("Dupont", 20)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(customers, result.getOrNull())
        verify(customerApiService).searchCustomers(eq("Dupont"), eq(20))
    }

    @Test
    fun `searchCustomers should return empty list when no results`() = runTest {
        // Given
        whenever(customerApiService.searchCustomers(eq("Unknown"), eq(20)))
            .thenReturn(Response.success(emptyList()))

        // When
        val result = repository.searchCustomers("Unknown", 20)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `searchCustomers should return failure on API error`() = runTest {
        // Given
        whenever(customerApiService.searchCustomers(eq("Dupont"), eq(20)))
            .thenReturn(Response.error(404, "".toResponseBody()))

        // When
        val result = repository.searchCustomers("Dupont", 20)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `searchCustomers should return failure on exception`() = runTest {
        // Given
        whenever(customerApiService.searchCustomers(any(), any()))
            .thenThrow(RuntimeException("Network error"))

        // When
        val result = repository.searchCustomers("Dupont", 20)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    // ===== getCustomerById() Tests =====

    @Test
    fun `getCustomerById should return success with customer`() = runTest {
        // Given
        val customer = Customer(id = 1L, firstName = "Jean", lastName = "Dupont", type = "STANDARD")
        whenever(customerApiService.getCustomerById(eq(1L)))
            .thenReturn(Response.success(customer))

        // When
        val result = repository.getCustomerById(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(customer, result.getOrNull())
        verify(customerApiService).getCustomerById(eq(1L))
    }

    @Test
    fun `getCustomerById should return failure when customer not found`() = runTest {
        // Given
        whenever(customerApiService.getCustomerById(eq(999L)))
            .thenReturn(Response.error(404, "".toResponseBody()))

        // When
        val result = repository.getCustomerById(999L)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Customer not found", result.exceptionOrNull()?.message)
    }

    // ===== createUninsuredCustomer() Tests =====

    @Test
    fun `createUninsuredCustomer with parameters should return success`() = runTest {
        // Given
        val createdCustomer = Customer(
            id = 1L,
            firstName = "Jean",
            lastName = "Dupont",
            phone = "0123456789",
            email = "jean@example.com",
            type = "STANDARD"
        )
        val expectedRequest = CreateCustomerRequest(
            firstName = "Jean",
            lastName = "Dupont",
            phone = "0123456789",
            email = "jean@example.com"
        )
        whenever(customerApiService.createUninsuredCustomer(any()))
            .thenReturn(Response.success(createdCustomer))

        // When
        val result = repository.createUninsuredCustomer(
            firstName = "Jean",
            lastName = "Dupont",
            phone = "0123456789",
            email = "jean@example.com"
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(createdCustomer, result.getOrNull())
        verify(customerApiService).createUninsuredCustomer(any())
    }

    @Test
    fun `createUninsuredCustomer with Customer object should return success`() = runTest {
        // Given
        val customer = Customer(
            firstName = "Jean",
            lastName = "Dupont",
            phone = "0123456789",
            email = "jean@example.com"
        )
        val createdCustomer = customer.copy(id = 1L, type = "STANDARD")
        whenever(customerApiService.createUninsuredCustomer(any()))
            .thenReturn(Response.success(createdCustomer))

        // When
        val result = repository.createUninsuredCustomer(customer)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(createdCustomer, result.getOrNull())
    }

    @Test
    fun `createUninsuredCustomer should return failure on API error`() = runTest {
        // Given
        whenever(customerApiService.createUninsuredCustomer(any()))
            .thenReturn(Response.error(400, "".toResponseBody()))

        // When
        val result = repository.createUninsuredCustomer(
            firstName = "Jean",
            lastName = "Dupont"
        )

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `createUninsuredCustomer should handle null phone and email`() = runTest {
        // Given
        val createdCustomer = Customer(
            id = 1L,
            firstName = "Jean",
            lastName = "Dupont",
            phone = null,
            email = null,
            type = "STANDARD"
        )
        whenever(customerApiService.createUninsuredCustomer(any()))
            .thenReturn(Response.success(createdCustomer))

        // When
        val result = repository.createUninsuredCustomer(
            firstName = "Jean",
            lastName = "Dupont",
            phone = null,
            email = null
        )

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull()?.phone)
        assertNull(result.getOrNull()?.email)
    }

    // ===== createAssureCustomer() Tests =====

    @Test
    fun `createAssureCustomer should return success`() = runTest {
        // Given
        val customer = Customer(
            firstName = "Marie",
            lastName = "Durand",
            phone = "0123456789",
            email = "marie@example.com",
            type = "ASSURE"
        )
        val createdCustomer = customer.copy(id = 2L)
        whenever(customerApiService.createAssureCustomer(eq(customer)))
            .thenReturn(Response.success(createdCustomer))

        // When
        val result = repository.createAssureCustomer(customer)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(createdCustomer, result.getOrNull())
        verify(customerApiService).createAssureCustomer(eq(customer))
    }

    @Test
    fun `createAssureCustomer should return failure on API error`() = runTest {
        // Given
        val customer = Customer(
            firstName = "Marie",
            lastName = "Durand",
            type = "ASSURE"
        )
        whenever(customerApiService.createAssureCustomer(any()))
            .thenReturn(Response.error(400, "".toResponseBody()))

        // When
        val result = repository.createAssureCustomer(customer)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to create assure customer") == true)
    }

    // ===== getDefaultCustomer() Tests =====

    @Test
    fun `getDefaultCustomer should return success and cache result`() = runTest {
        // Given
        val defaultCustomer = Customer(
            id = 100L,
            firstName = "Client",
            lastName = "Comptant",
            type = "STANDARD"
        )
        whenever(customerApiService.getDefaultCustomer())
            .thenReturn(Response.success(defaultCustomer))

        // When
        val result1 = repository.getDefaultCustomer()
        val result2 = repository.getDefaultCustomer() // Should use cache

        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals(defaultCustomer, result1.getOrNull())
        assertEquals(defaultCustomer, result2.getOrNull())
        // API should be called only once due to caching
        verify(customerApiService).getDefaultCustomer()
    }

    @Test
    fun `getDefaultCustomer with forceRefresh should bypass cache`() = runTest {
        // Given
        val defaultCustomer = Customer(
            id = 100L,
            firstName = "Client",
            lastName = "Comptant",
            type = "STANDARD"
        )
        whenever(customerApiService.getDefaultCustomer())
            .thenReturn(Response.success(defaultCustomer))

        // When
        repository.getDefaultCustomer() // First call, caches result
        val result = repository.getDefaultCustomer(forceRefresh = true) // Should bypass cache

        // Then
        assertTrue(result.isSuccess)
        // API should be called twice (no cache on second call)
        verify(customerApiService, org.mockito.kotlin.times(2)).getDefaultCustomer()
    }

    @Test
    fun `getDefaultCustomer should return failure on API error`() = runTest {
        // Given
        whenever(customerApiService.getDefaultCustomer())
            .thenReturn(Response.error(404, "".toResponseBody()))

        // When
        val result = repository.getDefaultCustomer()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to load default customer") == true)
    }

    // ===== getCustomerInsuranceData() Tests =====

    @Test
    fun `getCustomerInsuranceData should return success`() = runTest {
        // Given
        val insuranceData = InsuranceData(
            tiersPayants = emptyList(),
            plafondEncours = 100000,
            encours = 50000
        )
        whenever(customerApiService.getCustomerInsuranceData(eq(1L)))
            .thenReturn(Response.success(insuranceData))

        // When
        val result = repository.getCustomerInsuranceData(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(insuranceData, result.getOrNull())
        verify(customerApiService).getCustomerInsuranceData(eq(1L))
    }

    @Test
    fun `getCustomerInsuranceData should return failure on API error`() = runTest {
        // Given
        whenever(customerApiService.getCustomerInsuranceData(eq(1L)))
            .thenReturn(Response.error(404, "".toResponseBody()))

        // When
        val result = repository.getCustomerInsuranceData(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to load insurance data") == true)
    }

    // ===== getCustomerCarnetData() Tests =====

    @Test
    fun `getCustomerCarnetData should return success`() = runTest {
        // Given
        val carnetData = CarnetData(
            limiteCredit = 200000,
            encours = 75000,
            creditDisponible = 125000
        )
        whenever(customerApiService.getCustomerCarnetData(eq(1L)))
            .thenReturn(Response.success(carnetData))

        // When
        val result = repository.getCustomerCarnetData(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(carnetData, result.getOrNull())
        verify(customerApiService).getCustomerCarnetData(eq(1L))
    }

    @Test
    fun `getCustomerCarnetData should return failure on API error`() = runTest {
        // Given
        whenever(customerApiService.getCustomerCarnetData(eq(1L)))
            .thenReturn(Response.error(404, "".toResponseBody()))

        // When
        val result = repository.getCustomerCarnetData(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to load carnet data") == true)
    }

    // ===== getAyantDroits() Tests =====

    @Test
    fun `getAyantDroits should return success with list`() = runTest {
        // Given
        val ayantDroits = listOf(
            Customer(id = 2L, firstName = "Enfant1", lastName = "Dupont", type = "AYANT_DROIT"),
            Customer(id = 3L, firstName = "Enfant2", lastName = "Dupont", type = "AYANT_DROIT")
        )
        whenever(customerApiService.getAyantDroits(eq(1L)))
            .thenReturn(Response.success(ayantDroits))

        // When
        val result = repository.getAyantDroits(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(ayantDroits, result.getOrNull())
        assertEquals(2, result.getOrNull()?.size)
        verify(customerApiService).getAyantDroits(eq(1L))
    }

    @Test
    fun `getAyantDroits should return empty list when no ayants droit`() = runTest {
        // Given
        whenever(customerApiService.getAyantDroits(eq(1L)))
            .thenReturn(Response.success(emptyList()))

        // When
        val result = repository.getAyantDroits(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `getAyantDroits should return failure on API error`() = runTest {
        // Given
        whenever(customerApiService.getAyantDroits(eq(1L)))
            .thenReturn(Response.error(404, "".toResponseBody()))

        // When
        val result = repository.getAyantDroits(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to load ayants-droit") == true)
    }

    // ===== clearCache() Tests =====

    @Test
    fun `clearCache should clear cached default customer`() = runTest {
        // Given
        val defaultCustomer = Customer(
            id = 100L,
            firstName = "Client",
            lastName = "Comptant",
            type = "STANDARD"
        )
        whenever(customerApiService.getDefaultCustomer())
            .thenReturn(Response.success(defaultCustomer))

        // When
        repository.getDefaultCustomer() // Cache the result
        repository.clearCache() // Clear cache
        repository.getDefaultCustomer() // Should call API again

        // Then
        // API should be called twice (not using cache after clearCache)
        verify(customerApiService, org.mockito.kotlin.times(2)).getDefaultCustomer()
    }
}
