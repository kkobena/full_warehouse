package com.kobe.warehouse.sales.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kobe.warehouse.sales.data.model.Product
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.auth.Account
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.data.repository.ProductRepository
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.ui.viewmodel.ComptantSaleViewModel
import com.kobe.warehouse.sales.utils.TokenManager
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
 * Unit tests for ComptantSaleViewModel
 *
 * Tests:
 * - Product search
 * - Add product to cart
 * - Put on hold functionality
 * - Load existing sale
 * - Finalize sale
 */
@ExperimentalCoroutinesApi
class ComptantSaleViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var salesRepository: SalesRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var authRepository: AuthRepository

    @Mock
    private lateinit var tokenManager: TokenManager

    private lateinit var viewModel: ComptantSaleViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock token manager
        whenever(tokenManager.hasAuthority(any())).thenReturn(false)

        viewModel = ComptantSaleViewModel(
            salesRepository,
            productRepository,
            paymentRepository,
            authRepository,
            tokenManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `searchProducts should update products LiveData on success`() = runTest {
        // Given
        val query = "Paracetamol"
        val mockProducts = listOf(
            Product(id = 1, productName = "Paracetamol 500mg", regularUnitPrice = 100, currentStockQuantity = 50),
            Product(id = 2, productName = "Paracetamol 1g", regularUnitPrice = 150, currentStockQuantity = 30)
        )
        whenever(productRepository.searchProducts(query)).thenReturn(Result.success(mockProducts))

        // When
        viewModel.searchProducts(query)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val result = viewModel.products.value
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("Paracetamol 500mg", result[0].productName)
    }

    @Test
    fun `searchProducts with query less than 2 chars should clear products`() {
        // When
        viewModel.searchProducts("P")

        // Then
        val result = viewModel.products.value
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchProducts should update error on failure`() = runTest {
        // Given
        val query = "Paracetamol"
        val errorMessage = "Search failed"
        whenever(productRepository.searchProducts(query)).thenReturn(Result.failure(Exception(errorMessage)))

        // When
        viewModel.searchProducts(query)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val error = viewModel.errorMessage.value
        assertNotNull(error)
        assertTrue(error.contains("error") || error.contains("Erreur"))
    }

    @Test
    fun `addProductToCart should add product to cart when stock is sufficient`() {
        // Given
        val product = Product(
            id = 1,
            productName = "Paracetamol 500mg",
            regularUnitPrice = 100,
            currentStockQuantity = 50,
            productCode = "PARA500"
        )

        // When
        viewModel.addProductToCart(product, quantity = 2)

        // Then
        val sale = viewModel.currentSale.value
        assertNotNull(sale)
        assertTrue(sale.salesLines.isNotEmpty())
        assertEquals(1, sale.salesLines.size)
        assertEquals(2, sale.salesLines[0].quantitySold)
    }

    @Test
    fun `putOnHold should save sale as prevente on success`() = runTest {
        // Given
        val product = Product(
            id = 1,
            productName = "Paracetamol 500mg",
            regularUnitPrice = 100,
            currentStockQuantity = 50,
            productCode = "PARA500"
        )
        viewModel.addProductToCart(product, quantity = 2)

        val mockAccount = Account(id = 1, login = "cashier1")
        val savedSale = Sale(id = 100, numberTransaction = "PRV-001", salesAmount = 200)

        whenever(authRepository.getAccount()).thenReturn(Result.success(mockAccount))
        whenever(salesRepository.putCashSaleOnHold(any())).thenReturn(Result.success(savedSale))

        // When
        viewModel.putOnHold()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(salesRepository).putCashSaleOnHold(any())
        val result = viewModel.salePutOnHold.value
        assertNotNull(result)
        assertEquals("PRV-001", result.numberTransaction)
    }

    @Test
    fun `putOnHold should fail if cart is empty`() {
        // When
        viewModel.putOnHold()

        // Then
        val error = viewModel.errorMessage.value
        assertNotNull(error)
        assertTrue(error.contains("vide") || error.contains("empty"))
    }

    @Test
    fun `loadSale should load existing sale on success`() = runTest {
        // Given
        val saleId = 100L
        val saleDate = "2024-01-15"
        val mockSale = Sale(
            id = saleId,
            numberTransaction = "VNO-001",
            salesAmount = 5000
        )
        whenever(salesRepository.getSaleById(saleId, saleDate)).thenReturn(Result.success(mockSale))

        // When
        viewModel.loadSale(saleId, saleDate)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(salesRepository).getSaleById(saleId, saleDate)
        val result = viewModel.currentSale.value
        assertNotNull(result)
        assertEquals("VNO-001", result.numberTransaction)
        assertEquals(5000, result.salesAmount)
    }

    @Test
    fun `loadSale should update error on failure`() = runTest {
        // Given
        val saleId = 100L
        val saleDate = "2024-01-15"
        val errorMessage = "Sale not found"
        whenever(salesRepository.getSaleById(saleId, saleDate)).thenReturn(Result.failure(Exception(errorMessage)))

        // When
        viewModel.loadSale(saleId, saleDate)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val error = viewModel.errorMessage.value
        assertNotNull(error)
        assertTrue(error.contains("error") || error.contains("Erreur"))
    }

    @Test
    fun `resetAfterSale should clear cart and finalized sale`() {
        // Given
        val product = Product(
            id = 1,
            productName = "Paracetamol 500mg",
            regularUnitPrice = 100,
            currentStockQuantity = 50,
            productCode = "PARA500"
        )
        viewModel.addProductToCart(product, quantity = 2)

        // When
        viewModel.resetAfterSale()

        // Then
        val sale = viewModel.currentSale.value
        assertNotNull(sale)
        assertTrue(sale.salesLines.isEmpty())
        assertEquals(0, sale.salesAmount)
    }

    @Test
    fun `clearPutOnHoldResult should clear salePutOnHold`() {
        // When
        viewModel.clearPutOnHoldResult()

        // Then
        assertEquals(null, viewModel.salePutOnHold.value)
    }

    @Test
    fun `clearError should clear error message`() {
        // When
        viewModel.clearError()

        // Then
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun `cartTotal should be calculated from current sale`() {
        // Given
        val product1 = Product(
            id = 1,
            productName = "Product 1",
            regularUnitPrice = 100,
            currentStockQuantity = 50,
            productCode = "P1"
        )
        val product2 = Product(
            id = 2,
            productName = "Product 2",
            regularUnitPrice = 200,
            currentStockQuantity = 30,
            productCode = "P2"
        )

        // When
        viewModel.addProductToCart(product1, quantity = 2)
        viewModel.addProductToCart(product2, quantity = 1)

        // Then
        val sale = viewModel.currentSale.value
        assertNotNull(sale)
        assertEquals(400, sale.salesAmount) // (100*2) + (200*1) = 400
    }

    @Test
    fun `cartItemCount should reflect number of items in cart`() {
        // Given
        val product1 = Product(
            id = 1,
            productName = "Product 1",
            regularUnitPrice = 100,
            currentStockQuantity = 50,
            productCode = "P1"
        )
        val product2 = Product(
            id = 2,
            productName = "Product 2",
            regularUnitPrice = 200,
            currentStockQuantity = 30,
            productCode = "P2"
        )

        // When
        viewModel.addProductToCart(product1, quantity = 2)
        viewModel.addProductToCart(product2, quantity = 1)

        // Then
        val sale = viewModel.currentSale.value
        assertNotNull(sale)
        assertEquals(2, sale.salesLines.size)
    }
}
