package com.kobe.warehouse.sales.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.TiersPayantRepository
import com.kobe.warehouse.sales.domain.model.PrescriptionType
import com.kobe.warehouse.sales.domain.model.TiersPayant
import com.kobe.warehouse.sales.domain.model.TiersPayantType
import com.kobe.warehouse.sales.ui.viewmodel.InsuranceDataViewModel
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
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for InsuranceDataViewModel
 *
 * Tests:
 * - Tiers payant selection and management
 * - Prescription type selection
 * - Coverage rate calculation
 * - Insurance data validation
 */
@ExperimentalCoroutinesApi
class InsuranceDataViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var tiersPayantRepository: TiersPayantRepository

    @Mock
    private lateinit var customerRepository: CustomerRepository

    private lateinit var viewModel: InsuranceDataViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = InsuranceDataViewModel(
            tiersPayantRepository,
            customerRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Tiers Payant Tests ==========

    @Test
    fun `searchTiersPayants should update search results on success`() = runTest {
        val query = "MUGEFCI"
        val mockTiersPayants = listOf(
            TiersPayant(
                id = 1,
                name = "MUGEFCI",
                code = "MUG001",
                tauxCouverture = 80,
                type = TiersPayantType.PRINCIPAL
            )
        )
        whenever(tiersPayantRepository.searchTiersPayants(query)).thenReturn(Result.success(mockTiersPayants))

        viewModel.searchTiersPayants(query)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.tiersPayantSearchResults.value
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("MUGEFCI", result[0].name)
    }

    @Test
    fun `selectTiersPayantPrincipal should set tiers payant and coverage rate`() {
        val tiersPayant = TiersPayant(
            id = 1,
            name = "MUGEFCI",
            code = "MUG001",
            tauxCouverture = 80,
            type = TiersPayantType.PRINCIPAL
        )

        viewModel.selectTiersPayantPrincipal(tiersPayant)

        assertEquals(tiersPayant, viewModel.tiersPayantPrincipal.value)
        assertEquals(80, viewModel.tauxCouverturePrincipal.value)
        assertEquals(80, viewModel.totalCoverageRate.value)
        assertEquals(20, viewModel.clientShareRate.value)
    }

    @Test
    fun `updateTauxCouverturePrincipal should recalculate coverage rates`() {
        val tiersPayant = TiersPayant(
            id = 1,
            name = "MUGEFCI",
            code = "MUG001",
            tauxCouverture = 80,
            type = TiersPayantType.PRINCIPAL
        )
        viewModel.selectTiersPayantPrincipal(tiersPayant)

        viewModel.updateTauxCouverturePrincipal(70)

        assertEquals(70, viewModel.tauxCouverturePrincipal.value)
        assertEquals(70, viewModel.totalCoverageRate.value)
        assertEquals(30, viewModel.clientShareRate.value)
    }

    @Test
    fun `addTiersPayantComplementaire should add to list`() {
        val principal = TiersPayant(
            id = 1,
            name = "MUGEFCI",
            code = "MUG001",
            tauxCouverture = 70,
            type = TiersPayantType.PRINCIPAL
        )
        val complementaire = TiersPayant(
            id = 2,
            name = "Mutuelle Santé",
            code = "MUT001",
            tauxCouverture = 20,
            type = TiersPayantType.COMPLEMENTAIRE
        )

        viewModel.selectTiersPayantPrincipal(principal)
        viewModel.addTiersPayantComplementaire(complementaire)

        val result = viewModel.tiersPayantsComplementaires.value
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(90, viewModel.totalCoverageRate.value)
        assertEquals(10, viewModel.clientShareRate.value)
    }

    @Test
    fun `addTiersPayantComplementaire should fail if already added`() {
        val principal = TiersPayant(
            id = 1,
            name = "MUGEFCI",
            code = "MUG001",
            tauxCouverture = 80,
            type = TiersPayantType.PRINCIPAL
        )
        val complementaire = TiersPayant(
            id = 2,
            name = "Mutuelle",
            code = "MUT001",
            tauxCouverture = 20,
            type = TiersPayantType.COMPLEMENTAIRE
        )

        viewModel.selectTiersPayantPrincipal(principal)
        viewModel.addTiersPayantComplementaire(complementaire)
        viewModel.addTiersPayantComplementaire(complementaire) // Try to add again

        val error = viewModel.errorMessage.value
        assertNotNull(error)
        assertTrue(error.contains("déjà ajouté"))
    }

    @Test
    fun `removeTiersPayantComplementaire should remove from list`() {
        val principal = TiersPayant(
            id = 1,
            name = "MUGEFCI",
            code = "MUG001",
            tauxCouverture = 70,
            type = TiersPayantType.PRINCIPAL
        )
        val complementaire = TiersPayant(
            id = 2,
            name = "Mutuelle",
            code = "MUT001",
            tauxCouverture = 20,
            type = TiersPayantType.COMPLEMENTAIRE
        )

        viewModel.selectTiersPayantPrincipal(principal)
        viewModel.addTiersPayantComplementaire(complementaire)
        viewModel.removeTiersPayantComplementaire(complementaire)

        val result = viewModel.tiersPayantsComplementaires.value
        assertNotNull(result)
        assertEquals(0, result.size)
        assertEquals(70, viewModel.totalCoverageRate.value)
    }

    @Test
    fun `totalCoverageRate should not exceed 100 percent`() {
        val principal = TiersPayant(
            id = 1,
            name = "MUGEFCI",
            code = "MUG001",
            tauxCouverture = 80,
            type = TiersPayantType.PRINCIPAL
        )
        val complementaire = TiersPayant(
            id = 2,
            name = "Mutuelle",
            code = "MUT001",
            tauxCouverture = 30,
            type = TiersPayantType.COMPLEMENTAIRE
        )

        viewModel.selectTiersPayantPrincipal(principal)
        viewModel.addTiersPayantComplementaire(complementaire)

        // Total would be 110%, but should be capped at 100%
        assertEquals(100, viewModel.totalCoverageRate.value)
        assertEquals(0, viewModel.clientShareRate.value)

        // Should have error message
        val error = viewModel.errorMessage.value
        assertNotNull(error)
        assertTrue(error.contains("100%"))
    }

    // ========== Prescription Tests ==========

    @Test
    fun `setPrescriptionType should update prescription type`() {
        viewModel.setPrescriptionType(PrescriptionType.BON_PRISE_EN_CHARGE)

        assertEquals(PrescriptionType.BON_PRISE_EN_CHARGE, viewModel.prescriptionType.value)
        assertEquals(true, viewModel.isNumeroBonRequired.value)
    }

    @Test
    fun `setPrescriptionType ORDONNANCE should not require numero bon`() {
        viewModel.setPrescriptionType(PrescriptionType.ORDONNANCE)

        assertEquals(PrescriptionType.ORDONNANCE, viewModel.prescriptionType.value)
        assertEquals(false, viewModel.isNumeroBonRequired.value)
    }

    @Test
    fun `setNumeroBon should update numero bon`() {
        val numero = "BPC-12345"

        viewModel.setNumeroBon(numero)

        assertEquals(numero, viewModel.numeroBon.value)
    }

    // ========== Ayant Droit Tests ==========

    @Test
    fun `loadAyantDroits should update ayantDroitsList on success`() = runTest {
        val customerId = 1L
        val mockAyantDroits = listOf(
            Customer(id = 2, firstName = "Marie", lastName = "Doe"),
            Customer(id = 3, firstName = "Pierre", lastName = "Doe")
        )
        whenever(customerRepository.getAyantDroits(customerId)).thenReturn(Result.success(mockAyantDroits))

        viewModel.loadAyantDroits(customerId)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.ayantDroitsList.value
        assertNotNull(result)
        assertEquals(2, result.size)
    }

    @Test
    fun `selectAyantDroit should set ayant droit`() {
        val ayantDroit = Customer(id = 2, firstName = "Marie", lastName = "Doe")

        viewModel.selectAyantDroit(ayantDroit)

        assertEquals(ayantDroit, viewModel.ayantDroit.value)
    }

    @Test
    fun `clearAyantDroit should clear ayant droit`() {
        val ayantDroit = Customer(id = 2, firstName = "Marie", lastName = "Doe")
        viewModel.selectAyantDroit(ayantDroit)

        viewModel.clearAyantDroit()

        assertEquals(null, viewModel.ayantDroit.value)
    }

    // ========== Validation Tests ==========

    @Test
    fun `validateInsuranceData should fail if no tiers payant principal`() {
        val (isValid, error) = viewModel.validateInsuranceData()

        assertEquals(false, isValid)
        assertNotNull(error)
    }

    @Test
    fun `validateInsuranceData should fail if numero bon required but missing`() {
        val tiersPayant = TiersPayant(
            id = 1,
            name = "MUGEFCI",
            code = "MUG001",
            tauxCouverture = 80,
            type = TiersPayantType.PRINCIPAL
        )
        viewModel.selectTiersPayantPrincipal(tiersPayant)
        viewModel.setPrescriptionType(PrescriptionType.BON_PRISE_EN_CHARGE)
        // Don't set numero bon

        val (isValid, error) = viewModel.validateInsuranceData()

        assertEquals(false, isValid)
        assertNotNull(error)
        assertTrue(error.contains("numéro de bon"))
    }

    @Test
    fun `validateInsuranceData should succeed with valid data`() {
        val tiersPayant = TiersPayant(
            id = 1,
            name = "MUGEFCI",
            code = "MUG001",
            tauxCouverture = 80,
            type = TiersPayantType.PRINCIPAL
        )
        viewModel.selectTiersPayantPrincipal(tiersPayant)
        viewModel.setPrescriptionType(PrescriptionType.BON_PRISE_EN_CHARGE)
        viewModel.setNumeroBon("BPC-12345")

        val (isValid, error) = viewModel.validateInsuranceData()

        assertEquals(true, isValid)
        assertEquals(null, error)
    }

    @Test
    fun `buildInsuranceData should return null if no tiers payant`() {
        val customer = Customer(id = 1, firstName = "John", lastName = "Doe")

        val insuranceData = viewModel.buildInsuranceData(customer)

        assertEquals(null, insuranceData)
    }

    @Test
    fun `buildInsuranceData should return complete data when valid`() {
        val customer = Customer(id = 1, firstName = "John", lastName = "Doe")
        val tiersPayant = TiersPayant(
            id = 1,
            name = "MUGEFCI",
            code = "MUG001",
            tauxCouverture = 80,
            type = TiersPayantType.PRINCIPAL
        )
        viewModel.selectTiersPayantPrincipal(tiersPayant)
        viewModel.setPrescriptionType(PrescriptionType.ORDONNANCE)

        val insuranceData = viewModel.buildInsuranceData(customer)

        assertNotNull(insuranceData)
        assertEquals(tiersPayant, insuranceData.tiersPayantPrincipal)
        assertEquals(PrescriptionType.ORDONNANCE, insuranceData.prescriptionType)
        assertEquals(80, insuranceData.tauxCouverturePrincipal)
    }

    @Test
    fun `reset should clear all data`() {
        val tiersPayant = TiersPayant(
            id = 1,
            name = "MUGEFCI",
            code = "MUG001",
            tauxCouverture = 80,
            type = TiersPayantType.PRINCIPAL
        )
        viewModel.selectTiersPayantPrincipal(tiersPayant)
        viewModel.setPrescriptionType(PrescriptionType.BON_PRISE_EN_CHARGE)
        viewModel.setNumeroBon("BPC-12345")

        viewModel.reset()

        assertEquals(null, viewModel.tiersPayantPrincipal.value)
        assertEquals(0, viewModel.tauxCouverturePrincipal.value)
        assertEquals(PrescriptionType.ORDONNANCE, viewModel.prescriptionType.value)
        assertEquals(null, viewModel.numeroBon.value)
    }
}
