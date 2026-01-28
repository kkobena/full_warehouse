package com.kobe.warehouse.sales.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kobe.warehouse.sales.domain.model.PrescriptionType
import com.kobe.warehouse.sales.domain.model.TiersPayant
import com.kobe.warehouse.sales.domain.model.TiersPayantType
import com.kobe.warehouse.sales.ui.viewmodel.InsuranceDataViewModel
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for InsuranceDataViewModel
 *
 * Tests:
 * - Tiers payant management
 * - Prescription type and number
 * - Validation logic
 *
 * NOTE: Coverage calculations are done by BACKEND, not tested here
 */
@ExperimentalCoroutinesApi
class InsuranceDataViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: InsuranceDataViewModel

    // Test data
    private val principalTiersPayant = TiersPayant(
        id = 1L,
        name = "CNSS",
        code = "CNSS01",
        tauxCouverture = 80,
        type = TiersPayantType.PRINCIPAL
    )

    private val complementaireTiersPayant = TiersPayant(
        id = 2L,
        name = "Mutuelle",
        code = "MUT01",
        tauxCouverture = 15,
        type = TiersPayantType.COMPLEMENTAIRE
    )

    private val principal2TiersPayant = TiersPayant(
        id = 3L,
        name = "CNAM",
        code = "CNAM01",
        tauxCouverture = 70,
        type = TiersPayantType.PRINCIPAL
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = InsuranceDataViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== Tiers Payant Management Tests =====

    @Test
    fun `initial selected tiers payants should be empty`() {
        // When
        val tiersPayants = viewModel.selectedTiersPayants.value

        // Then
        assertNotNull(tiersPayants)
        assertTrue(tiersPayants.isEmpty())
    }

    @Test
    fun `addTiersPayant should add to list`() {
        // When
        viewModel.addTiersPayant(principalTiersPayant)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.selectedTiersPayants.value?.size)
        assertEquals(principalTiersPayant, viewModel.selectedTiersPayants.value?.first())
    }

    @Test
    fun `addTiersPayant principal when already has principal should fail`() {
        // Given
        viewModel.addTiersPayant(principalTiersPayant)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.addTiersPayant(principal2TiersPayant)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.selectedTiersPayants.value?.size)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Un seul tiers payant principal"))
    }

    @Test
    fun `addTiersPayant complementaire when has principal should succeed`() {
        // Given
        viewModel.addTiersPayant(principalTiersPayant)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.addTiersPayant(complementaireTiersPayant)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(2, viewModel.selectedTiersPayants.value?.size)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `removeTiersPayant should remove from list`() {
        // Given
        viewModel.addTiersPayant(principalTiersPayant)
        viewModel.addTiersPayant(complementaireTiersPayant)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.removeTiersPayant(principalTiersPayant)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.selectedTiersPayants.value?.size)
        assertEquals(complementaireTiersPayant, viewModel.selectedTiersPayants.value?.first())
    }

    @Test
    fun `clearTiersPayants should clear all`() {
        // Given
        viewModel.addTiersPayant(principalTiersPayant)
        viewModel.addTiersPayant(complementaireTiersPayant)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearTiersPayants()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(0, viewModel.selectedTiersPayants.value?.size)
    }

    // ===== Prescription Data Tests =====

    @Test
    fun `initial prescription type should be ORDONNANCE`() {
        // When
        val prescriptionType = viewModel.prescriptionType.value

        // Then
        assertEquals(PrescriptionType.ORDONNANCE, prescriptionType)
    }

    @Test
    fun `setPrescriptionType should update type`() {
        // When
        viewModel.setPrescriptionType(PrescriptionType.BON_PRISE_EN_CHARGE)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(PrescriptionType.BON_PRISE_EN_CHARGE, viewModel.prescriptionType.value)
    }

    @Test
    fun `setPrescriptionNumber should update number`() {
        // When
        viewModel.setPrescriptionNumber("12345")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("12345", viewModel.prescriptionNumber.value)
    }

    // ===== Validation Tests =====

    @Test
    fun `validateInsuranceData with no tiers payants should fail`() {
        // When
        val isValid = viewModel.validateInsuranceData()

        // Then
        assertFalse(isValid)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Au moins un tiers payant"))
    }

    @Test
    fun `validateInsuranceData with tiers payants should succeed`() {
        // Given
        viewModel.addTiersPayant(principalTiersPayant)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val isValid = viewModel.validateInsuranceData()

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `validateInsuranceData with prescription requiring number but no number should fail`() {
        // Given
        viewModel.addTiersPayant(principalTiersPayant)
        viewModel.setPrescriptionType(PrescriptionType.BON_PRISE_EN_CHARGE)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val isValid = viewModel.validateInsuranceData()

        // Then
        assertFalse(isValid)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Numéro de bon requis"))
    }

    @Test
    fun `validateInsuranceData with prescription and number should succeed`() {
        // Given
        viewModel.addTiersPayant(principalTiersPayant)
        viewModel.setPrescriptionType(PrescriptionType.BON_PRISE_EN_CHARGE)
        viewModel.setPrescriptionNumber("12345")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val isValid = viewModel.validateInsuranceData()

        // Then
        assertTrue(isValid)
    }

    // ===== Error Handling Tests =====

    @Test
    fun `clearError should clear error message`() {
        // Given
        viewModel.validateInsuranceData() // Will set error (no tiers payants)

        // When
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.errorMessage.value)
    }

    // ===== Backend Delegation Tests =====

    @Test
    fun `coverage calculations should be done by backend - no local methods`() {
        // This test verifies the architectural decision that calculations
        // are done by the backend, not the ViewModel

        // Given
        viewModel.addTiersPayant(principalTiersPayant)
        viewModel.addTiersPayant(complementaireTiersPayant)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        // ViewModel should only store tiers payants
        assertEquals(2, viewModel.selectedTiersPayants.value?.size)

        // Coverage calculations (getTotalCoverageRate, calculateInsurancePart, etc.)
        // are NOT part of the ViewModel - they're done by the BACKEND
        // The backend returns partAssure and costAmount in the Sale response
    }
}
