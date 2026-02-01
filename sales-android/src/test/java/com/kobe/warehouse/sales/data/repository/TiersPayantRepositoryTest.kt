package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.TiersPayantApiService
import com.kobe.warehouse.sales.domain.model.TiersPayant
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
import kotlin.test.assertTrue

/**
 * Unit tests for TiersPayantRepository
 * Tests tiers payant (insurance provider) operations
 */
@ExperimentalCoroutinesApi
class TiersPayantRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var tiersPayantApiService: TiersPayantApiService

    private lateinit var repository: TiersPayantRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        repository = TiersPayantRepository(tiersPayantApiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== getAllTiersPayants() Tests =====

    @Test
    fun `getAllTiersPayants should return success with list`() = runTest {
        // Given
        val tiersPayants = listOf(
            TiersPayant(
                id = 1L,
                name = "MUGEF-CI",
                code = "MUG001"
            ),
            TiersPayant(
                id = 2L,
                name = "CARNET-CI",
                code = "CAR001"
            )
        )
        whenever(tiersPayantApiService.getAllTiersPayants())
            .thenReturn(Response.success(tiersPayants))

        // When
        val result = repository.getAllTiersPayants()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(tiersPayants, result.getOrNull())
        assertEquals(2, result.getOrNull()?.size)
        verify(tiersPayantApiService).getAllTiersPayants()
    }

    @Test
    fun `getAllTiersPayants should return empty list when no tiers payants`() = runTest {
        // Given
        whenever(tiersPayantApiService.getAllTiersPayants())
            .thenReturn(Response.success(emptyList()))

        // When
        val result = repository.getAllTiersPayants()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `getAllTiersPayants should return failure on API error`() = runTest {
        // Given
        whenever(tiersPayantApiService.getAllTiersPayants())
            .thenReturn(Response.error(500, "".toResponseBody()))

        // When
        val result = repository.getAllTiersPayants()

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
        assertTrue(result.exceptionOrNull()?.message?.contains("Erreur") == true)
    }

    @Test
    fun `getAllTiersPayants should return failure on exception`() = runTest {
        // Given
        whenever(tiersPayantApiService.getAllTiersPayants())
            .thenThrow(RuntimeException("Network error"))

        // When
        val result = repository.getAllTiersPayants()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    // ===== searchTiersPayants() Tests =====

    @Test
    fun `searchTiersPayants should return success with matching results`() = runTest {
        // Given
        val searchResults = listOf(
            TiersPayant(
                id = 1L,
                name = "MUGEF-CI",
                code = "MUG001"
            )
        )
        whenever(tiersPayantApiService.searchTiersPayants(eq("MUGEF")))
            .thenReturn(Response.success(searchResults))

        // When
        val result = repository.searchTiersPayants("MUGEF")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(searchResults, result.getOrNull())
        assertEquals(1, result.getOrNull()?.size)
        verify(tiersPayantApiService).searchTiersPayants(eq("MUGEF"))
    }

    @Test
    fun `searchTiersPayants should return empty list when no matches`() = runTest {
        // Given
        whenever(tiersPayantApiService.searchTiersPayants(eq("Unknown")))
            .thenReturn(Response.success(emptyList()))

        // When
        val result = repository.searchTiersPayants("Unknown")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `searchTiersPayants should return failure on API error`() = runTest {
        // Given
        whenever(tiersPayantApiService.searchTiersPayants(any()))
            .thenReturn(Response.error(404, "".toResponseBody()))

        // When
        val result = repository.searchTiersPayants("MUGEF")

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    // ===== getTiersPayantById() Tests =====

    @Test
    fun `getTiersPayantById should return success with tiers payant`() = runTest {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "MUGEF-CI",
            code = "MUG001"
        )
        whenever(tiersPayantApiService.getTiersPayantById(eq(1L)))
            .thenReturn(Response.success(tiersPayant))

        // When
        val result = repository.getTiersPayantById(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(tiersPayant, result.getOrNull())
        verify(tiersPayantApiService).getTiersPayantById(eq(1L))
    }

    @Test
    fun `getTiersPayantById should return failure when not found`() = runTest {
        // Given
        whenever(tiersPayantApiService.getTiersPayantById(eq(999L)))
            .thenReturn(Response.error(404, "".toResponseBody()))

        // When
        val result = repository.getTiersPayantById(999L)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    // ===== getTiersPayantsForCustomer() Tests =====

    @Test
    fun `getTiersPayantsForCustomer should return success with customer insurance list`() = runTest {
        // Given
        val tiersPayants = listOf(
            TiersPayant(
                id = 1L,
                name = "MUGEF-CI",
                code = "MUG001"
            ),
            TiersPayant(
                id = 2L,
                name = "MUGEF-CI Compl",
                code = "MUGC001"
            )
        )
        whenever(tiersPayantApiService.getTiersPayantsForCustomer(eq(1L)))
            .thenReturn(Response.success(tiersPayants))

        // When
        val result = repository.getTiersPayantsForCustomer(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(tiersPayants, result.getOrNull())
        assertEquals(2, result.getOrNull()?.size)
        verify(tiersPayantApiService).getTiersPayantsForCustomer(eq(1L))
    }

    @Test
    fun `getTiersPayantsForCustomer should return empty list when customer has no insurance`() = runTest {
        // Given
        whenever(tiersPayantApiService.getTiersPayantsForCustomer(eq(1L)))
            .thenReturn(Response.success(emptyList()))

        // When
        val result = repository.getTiersPayantsForCustomer(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `getTiersPayantsForCustomer should return failure on API error`() = runTest {
        // Given
        whenever(tiersPayantApiService.getTiersPayantsForCustomer(eq(1L)))
            .thenReturn(Response.error(404, "".toResponseBody()))

        // When
        val result = repository.getTiersPayantsForCustomer(1L)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    // ===== validateTiersPayantForCustomer() Tests =====

    @Test
    fun `validateTiersPayantForCustomer should return true when valid`() = runTest {
        // Given
        whenever(tiersPayantApiService.validateTiersPayantForCustomer(eq(1L), eq(1L)))
            .thenReturn(Response.success(true))

        // When
        val result = repository.validateTiersPayantForCustomer(1L, 1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        verify(tiersPayantApiService).validateTiersPayantForCustomer(eq(1L), eq(1L))
    }

    @Test
    fun `validateTiersPayantForCustomer should return false when invalid`() = runTest {
        // Given
        whenever(tiersPayantApiService.validateTiersPayantForCustomer(eq(1L), eq(2L)))
            .thenReturn(Response.success(false))

        // When
        val result = repository.validateTiersPayantForCustomer(1L, 2L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun `validateTiersPayantForCustomer should return failure on API error`() = runTest {
        // Given
        whenever(tiersPayantApiService.validateTiersPayantForCustomer(eq(1L), eq(1L)))
            .thenReturn(Response.error(400, "".toResponseBody()))

        // When
        val result = repository.validateTiersPayantForCustomer(1L, 1L)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    // ===== createTiersPayant() Tests =====

    @Test
    fun `createTiersPayant should return success with created tiers payant`() = runTest {
        // Given
        val newTiersPayant = com.kobe.warehouse.sales.data.model.TiersPayant(
            name = "NEW-ASSURANCE",
            fullName = "Nouvelle Assurance",
            categorie = "ASSURANCE",
            codeOrganisme = "NA001",
            telephone = "0123456789",
            email = "contact@new.ci"
        )
        val createdTiersPayant = newTiersPayant.copy(id = 10L, statut = "ENABLE")
        whenever(tiersPayantApiService.createTiersPayant(eq(newTiersPayant)))
            .thenReturn(Response.success(createdTiersPayant))

        // When
        val result = repository.createTiersPayant(newTiersPayant)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(createdTiersPayant, result.getOrNull())
        assertEquals(10L, result.getOrNull()?.id)
        verify(tiersPayantApiService).createTiersPayant(eq(newTiersPayant))
    }

    @Test
    fun `createTiersPayant should return failure on API error`() = runTest {
        // Given
        val newTiersPayant = com.kobe.warehouse.sales.data.model.TiersPayant(
            name = "NEW-ASSURANCE",
            fullName = "Nouvelle Assurance",
            categorie = "ASSURANCE"
        )
        whenever(tiersPayantApiService.createTiersPayant(any()))
            .thenReturn(Response.error(400, "".toResponseBody()))

        // When
        val result = repository.createTiersPayant(newTiersPayant)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Erreur lors de la création") == true)
    }

    @Test
    fun `createTiersPayant should return failure on exception`() = runTest {
        // Given
        val newTiersPayant = com.kobe.warehouse.sales.data.model.TiersPayant(
            name = "NEW-ASSURANCE",
            fullName = "Nouvelle Assurance",
            categorie = "ASSURANCE"
        )
        whenever(tiersPayantApiService.createTiersPayant(any()))
            .thenThrow(RuntimeException("Network error"))

        // When
        val result = repository.createTiersPayant(newTiersPayant)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `createTiersPayant should handle all optional fields`() = runTest {
        // Given
        val newTiersPayant = com.kobe.warehouse.sales.data.model.TiersPayant(
            name = "NEW-ASSURANCE",
            fullName = "Nouvelle Assurance",
            categorie = "ASSURANCE",
            codeOrganisme = "NA001",
            telephone = "0123456789",
            email = "contact@new.ci",
            plafondConso = 100000,
            plafondAbsolu = true,
            nbreBordereaux = 5
        )
        val createdTiersPayant = newTiersPayant.copy(id = 10L, statut = "ENABLE")
        whenever(tiersPayantApiService.createTiersPayant(eq(newTiersPayant)))
            .thenReturn(Response.success(createdTiersPayant))

        // When
        val result = repository.createTiersPayant(newTiersPayant)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(100000, result.getOrNull()?.plafondConso)
        assertEquals(true, result.getOrNull()?.plafondAbsolu)
        assertEquals(5, result.getOrNull()?.nbreBordereaux)
    }

    // ===== updateTiersPayant() Tests =====

    @Test
    fun `updateTiersPayant should return success with updated tiers payant`() = runTest {
        // Given
        val updatedTiersPayant = com.kobe.warehouse.sales.data.model.TiersPayant(
            id = 1L,
            name = "MUGEF-CI",
            fullName = "Mutuelle Générale des Fonctionnaires - Mise à jour",
            categorie = "ASSURANCE",
            codeOrganisme = "MUGEF001",
            telephone = "9876543210",
            email = "nouveau@mugef.ci",
            statut = "ENABLE"
        )
        whenever(tiersPayantApiService.updateTiersPayant(eq(1L), eq(updatedTiersPayant)))
            .thenReturn(Response.success(updatedTiersPayant))

        // When
        val result = repository.updateTiersPayant(1L, updatedTiersPayant)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(updatedTiersPayant, result.getOrNull())
        assertEquals("9876543210", result.getOrNull()?.telephone)
        verify(tiersPayantApiService).updateTiersPayant(eq(1L), eq(updatedTiersPayant))
    }

    @Test
    fun `updateTiersPayant should return failure on API error`() = runTest {
        // Given
        val updatedTiersPayant = com.kobe.warehouse.sales.data.model.TiersPayant(
            id = 1L,
            name = "MUGEF-CI",
            fullName = "Updated",
            categorie = "ASSURANCE"
        )
        whenever(tiersPayantApiService.updateTiersPayant(eq(1L), any()))
            .thenReturn(Response.error(404, "".toResponseBody()))

        // When
        val result = repository.updateTiersPayant(1L, updatedTiersPayant)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Erreur lors de la mise à jour") == true)
    }

    @Test
    fun `updateTiersPayant should return failure on exception`() = runTest {
        // Given
        val updatedTiersPayant = com.kobe.warehouse.sales.data.model.TiersPayant(
            id = 1L,
            name = "MUGEF-CI",
            fullName = "Updated",
            categorie = "ASSURANCE"
        )
        whenever(tiersPayantApiService.updateTiersPayant(eq(1L), any()))
            .thenThrow(RuntimeException("Network error"))

        // When
        val result = repository.updateTiersPayant(1L, updatedTiersPayant)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `updateTiersPayant should handle status change`() = runTest {
        // Given
        val updatedTiersPayant = com.kobe.warehouse.sales.data.model.TiersPayant(
            id = 1L,
            name = "MUGEF-CI",
            fullName = "Mutuelle Générale des Fonctionnaires",
            categorie = "ASSURANCE",
            statut = "DISABLE" // Changed status
        )
        whenever(tiersPayantApiService.updateTiersPayant(eq(1L), eq(updatedTiersPayant)))
            .thenReturn(Response.success(updatedTiersPayant))

        // When
        val result = repository.updateTiersPayant(1L, updatedTiersPayant)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("DISABLE", result.getOrNull()?.statut)
    }

    @Test
    fun `updateTiersPayant should handle plafond updates`() = runTest {
        // Given
        val updatedTiersPayant = com.kobe.warehouse.sales.data.model.TiersPayant(
            id = 1L,
            name = "MUGEF-CI",
            fullName = "Mutuelle Générale des Fonctionnaires",
            categorie = "ASSURANCE",
            plafondConso = 200000, // Updated plafond
            plafondAbsolu = false,
            nbreBordereaux = 10
        )
        whenever(tiersPayantApiService.updateTiersPayant(eq(1L), eq(updatedTiersPayant)))
            .thenReturn(Response.success(updatedTiersPayant))

        // When
        val result = repository.updateTiersPayant(1L, updatedTiersPayant)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(200000, result.getOrNull()?.plafondConso)
        assertEquals(false, result.getOrNull()?.plafondAbsolu)
        assertEquals(10, result.getOrNull()?.nbreBordereaux)
    }
}
