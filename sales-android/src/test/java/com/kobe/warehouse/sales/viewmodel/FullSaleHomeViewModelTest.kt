package com.kobe.warehouse.sales.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.ui.viewmodel.FullSaleHomeViewModel
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
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for FullSaleHomeViewModel
 *
 * Tests:
 * - Loading ongoing sales
 * - Loading preventes
 * - Search functionality
 * - Delete sale
 * - Error handling
 */
@ExperimentalCoroutinesApi
class FullSaleHomeViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var salesRepository: SalesRepository

    private lateinit var viewModel: FullSaleHomeViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = FullSaleHomeViewModel(salesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadOngoingSales should update ongoingSales LiveData on success`() = runTest {
        // Given
        val mockSales = listOf(
            Sale(id = 1, numberTransaction = "VNO-001", salesAmount = 5000),
            Sale(id = 2, numberTransaction = "VNO-002", salesAmount = 3000)
        )
        whenever(salesRepository.getSales(any())).thenReturn(Result.success(mockSales))

        // When
        viewModel.loadOngoingSales()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val result = viewModel.ongoingSales.value
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("VNO-001", result[0].numberTransaction)
    }

    @Test
    fun `loadOngoingSales should update error on failure`() = runTest {
        // Given
        val errorMessage = "Network error"
        whenever(salesRepository.getSales(any())).thenReturn(Result.failure(Exception(errorMessage)))

        // When
        viewModel.loadOngoingSales()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val error = viewModel.ongoingError.value
        assertNotNull(error)
        assertTrue(error.contains("error") || error.contains("Erreur"))
    }

    @Test
    fun `loadPreventes should update preventes LiveData on success`() = runTest {
        // Given
        val mockPreventes = listOf(
            Sale(id = 3, numberTransaction = "PRV-001", salesAmount = 2000),
            Sale(id = 4, numberTransaction = "PRV-002", salesAmount = 1500)
        )
        whenever(salesRepository.getPreventes(any())).thenReturn(Result.success(mockPreventes))

        // When
        viewModel.loadPreventes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val result = viewModel.preventes.value
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("PRV-001", result[0].numberTransaction)
    }

    @Test
    fun `loadPreventes should update error on failure`() = runTest {
        // Given
        val errorMessage = "Server error"
        whenever(salesRepository.getPreventes(any())).thenReturn(Result.failure(Exception(errorMessage)))

        // When
        viewModel.loadPreventes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val error = viewModel.preventesError.value
        assertNotNull(error)
        assertTrue(error.contains("error") || error.contains("Erreur"))
    }

    @Test
    fun `searchOngoingSales should call loadOngoingSales with query`() = runTest {
        // Given
        val searchQuery = "VNO"
        val mockSales = listOf(Sale(id = 1, numberTransaction = "VNO-001", salesAmount = 5000))
        whenever(salesRepository.getSales(searchQuery)).thenReturn(Result.success(mockSales))

        // When
        viewModel.searchOngoingSales(searchQuery)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(salesRepository).getSales(searchQuery)
    }

    @Test
    fun `searchPreventes should call loadPreventes with query`() = runTest {
        // Given
        val searchQuery = "PRV"
        val mockPreventes = listOf(Sale(id = 3, numberTransaction = "PRV-001", salesAmount = 2000))
        whenever(salesRepository.getPreventes(searchQuery)).thenReturn(Result.success(mockPreventes))

        // When
        viewModel.searchPreventes(searchQuery)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(salesRepository).getPreventes(searchQuery)
    }

    @Test
    fun `deleteSale should refresh ongoing sales on success`() = runTest {
        // Given
        val saleId = 1L
        val saleDate = "2024-01-15"
        whenever(salesRepository.deleteSale(saleId, saleDate)).thenReturn(Result.success(Unit))
        whenever(salesRepository.getSales(any())).thenReturn(Result.success(emptyList()))

        // When
        viewModel.deleteSale(saleId, saleDate, isPrevente = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(salesRepository).deleteSale(saleId, saleDate)
        verify(salesRepository).getSales(any()) // Refresh called
    }

    @Test
    fun `deleteSale should refresh preventes on success when isPrevente is true`() = runTest {
        // Given
        val saleId = 3L
        val saleDate = "2024-01-15"
        whenever(salesRepository.deleteSale(saleId, saleDate)).thenReturn(Result.success(Unit))
        whenever(salesRepository.getPreventes(any())).thenReturn(Result.success(emptyList()))

        // When
        viewModel.deleteSale(saleId, saleDate, isPrevente = true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(salesRepository).deleteSale(saleId, saleDate)
        verify(salesRepository).getPreventes(any()) // Refresh called
    }

    @Test
    fun `deleteSale should update error on failure`() = runTest {
        // Given
        val saleId = 1L
        val saleDate = "2024-01-15"
        val errorMessage = "Delete failed"
        whenever(salesRepository.deleteSale(saleId, saleDate)).thenReturn(Result.failure(Exception(errorMessage)))

        // When
        viewModel.deleteSale(saleId, saleDate, isPrevente = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val error = viewModel.ongoingError.value
        assertNotNull(error)
    }

    @Test
    fun `refreshOngoingSales should reload ongoing sales`() = runTest {
        // Given
        val mockSales = listOf(Sale(id = 1, numberTransaction = "VNO-001", salesAmount = 5000))
        whenever(salesRepository.getSales(any())).thenReturn(Result.success(mockSales))

        // When
        viewModel.refreshOngoingSales()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(salesRepository).getSales(any())
    }

    @Test
    fun `refreshPreventes should reload preventes`() = runTest {
        // Given
        val mockPreventes = listOf(Sale(id = 3, numberTransaction = "PRV-001", salesAmount = 2000))
        whenever(salesRepository.getPreventes(any())).thenReturn(Result.success(mockPreventes))

        // When
        viewModel.refreshPreventes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(salesRepository).getPreventes(any())
    }

    @Test
    fun `clearOngoingError should clear error message`() {
        // When
        viewModel.clearOngoingError()

        // Then
        assertEquals(null, viewModel.ongoingError.value)
    }

    @Test
    fun `clearPreventesError should clear error message`() {
        // When
        viewModel.clearPreventesError()

        // Then
        assertEquals(null, viewModel.preventesError.value)
    }
}
