package com.kobe.warehouse.sales.domain.model

import com.kobe.warehouse.sales.data.model.Customer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for SaleType sealed class
 * Tests the behavior of different sale types and customer requirements
 */
class SaleTypeTest {

    // ===== Comptant Tests =====

    @Test
    fun `Comptant should not require customer`() {
        // Given
        val saleType = SaleType.Comptant

        // Then
        assertFalse(saleType.requiresCustomer())
    }

    @Test
    fun `Comptant should always have customer (nullable behavior)`() {
        // Given
        val saleType = SaleType.Comptant

        // Then
        assertTrue(saleType.hasCustomer())  // Always true for Comptant
    }

    @Test
    fun `Comptant getCustomer should return null`() {
        // Given
        val saleType = SaleType.Comptant

        // Then
        assertNull(saleType.getCustomer())
    }

    @Test
    fun `Comptant toString should return COMPTANT`() {
        // Given
        val saleType = SaleType.Comptant

        // Then
        assertEquals("COMPTANT", saleType.toString())
    }

    @Test
    fun `Comptant getDisplayName should return Vente Comptant`() {
        // Given
        val saleType = SaleType.Comptant

        // Then
        assertEquals("Vente Comptant", saleType.getDisplayName())
    }

    // ===== Assurance Tests =====

    @Test
    fun `Assurance should require customer`() {
        // Given
        val customer = createTestCustomer()
        val saleType = SaleType.Assurance(customer, emptyList())

        // Then
        assertTrue(saleType.requiresCustomer())
    }

    @Test
    fun `Assurance with null customer should not have customer`() {
        // Given
        val saleType = SaleType.Assurance(null, emptyList())

        // Then
        assertFalse(saleType.hasCustomer())
    }

    @Test
    fun `Assurance with customer should have customer`() {
        // Given
        val customer = createTestCustomer()
        val saleType = SaleType.Assurance(customer, emptyList())

        // Then
        assertTrue(saleType.hasCustomer())
    }

    @Test
    fun `Assurance getCustomer should return customer`() {
        // Given
        val customer = createTestCustomer()
        val saleType = SaleType.Assurance(customer, emptyList())

        // Then
        assertEquals(customer, saleType.getCustomer())
    }

    @Test
    fun `Assurance with null customer getCustomer should return null`() {
        // Given
        val saleType = SaleType.Assurance(null, emptyList())

        // Then
        assertNull(saleType.getCustomer())
    }

    @Test
    fun `Assurance toString should return ASSURANCE`() {
        // Given
        val saleType = SaleType.Assurance(null, emptyList())

        // Then
        assertEquals("ASSURANCE", saleType.toString())
    }

    @Test
    fun `Assurance getDisplayName should return Vente Assurance`() {
        // Given
        val saleType = SaleType.Assurance(null, emptyList())

        // Then
        assertEquals("Vente Assurance", saleType.getDisplayName())
    }

    @Test
    fun `Assurance getPrincipalTiersPayant should return first tiers payant`() {
        // Given
        val tiersPayant1 = TiersPayant(1L, "MUGEF-CI", "R0")
        val tiersPayant2 = TiersPayant(2L, "MUGEF-CI Compl", "C1")
        val saleType = SaleType.Assurance(createTestCustomer(), listOf(tiersPayant1, tiersPayant2))

        // Then
        assertEquals(tiersPayant1, saleType.getPrincipalTiersPayant())
    }

    @Test
    fun `Assurance getComplementaireTiersPayants should return all except first`() {
        // Given
        val tiersPayant1 = TiersPayant(1L, "MUGEF-CI", "R0")
        val tiersPayant2 = TiersPayant(2L, "MUGEF-CI Compl", "C1")
        val tiersPayant3 = TiersPayant(3L, "MUGEF-CI Compl 2", "C2")
        val saleType = SaleType.Assurance(
            createTestCustomer(),
            listOf(tiersPayant1, tiersPayant2, tiersPayant3)
        )

        // When
        val complementaires = saleType.getComplementaireTiersPayants()

        // Then
        assertEquals(2, complementaires.size)
        assertEquals(tiersPayant2, complementaires[0])
        assertEquals(tiersPayant3, complementaires[1])
    }

    // ===== Carnet Tests =====

    @Test
    fun `Carnet should require customer`() {
        // Given
        val customer = createTestCustomer()
        val saleType = SaleType.Carnet(customer)

        // Then
        assertTrue(saleType.requiresCustomer())
    }

    @Test
    fun `Carnet with null customer should not have customer`() {
        // Given
        val saleType = SaleType.Carnet(null)

        // Then
        assertFalse(saleType.hasCustomer())
    }

    @Test
    fun `Carnet with customer should have customer`() {
        // Given
        val customer = createTestCustomer()
        val saleType = SaleType.Carnet(customer)

        // Then
        assertTrue(saleType.hasCustomer())
    }

    @Test
    fun `Carnet getCustomer should return customer`() {
        // Given
        val customer = createTestCustomer()
        val saleType = SaleType.Carnet(customer)

        // Then
        assertEquals(customer, saleType.getCustomer())
    }

    @Test
    fun `Carnet with null customer getCustomer should return null`() {
        // Given
        val saleType = SaleType.Carnet(null)

        // Then
        assertNull(saleType.getCustomer())
    }

    @Test
    fun `Carnet toString should return CARNET`() {
        // Given
        val saleType = SaleType.Carnet(null)

        // Then
        assertEquals("CARNET", saleType.toString())
    }

    @Test
    fun `Carnet getDisplayName should return Vente Carnet`() {
        // Given
        val saleType = SaleType.Carnet(null)

        // Then
        assertEquals("Vente Carnet", saleType.getDisplayName())
    }

    // ===== Helper Methods =====

    private fun createTestCustomer(
        id: Long = 1L,
        firstName: String = "Jean",
        lastName: String = "Dupont"
    ): Customer {
        return Customer(
            id = id,
            firstName = firstName,
            lastName = lastName,
            type = "ASSURE"
        )
    }
}
