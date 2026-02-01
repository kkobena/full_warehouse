package com.kobe.warehouse.sales.data.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for PrioriteTiersPayant enum
 * Tests enum values, conversion methods, and helper functions
 */
class PrioriteTiersPayantTest {

    // ===== Enum Values Tests =====

    @Test
    fun `R0 should have correct value and code`() {
        // Given
        val priority = PrioriteTiersPayant.R0

        // Then
        assertEquals(0, priority.value)
        assertEquals("R0", priority.code)
    }

    @Test
    fun `R1 should have correct value and code`() {
        // Given
        val priority = PrioriteTiersPayant.R1

        // Then
        assertEquals(1, priority.value)
        assertEquals("C1", priority.code)
    }

    @Test
    fun `R2 should have correct value and code`() {
        // Given
        val priority = PrioriteTiersPayant.R2

        // Then
        assertEquals(2, priority.value)
        assertEquals("C2", priority.code)
    }

    @Test
    fun `R3 should have correct value and code`() {
        // Given
        val priority = PrioriteTiersPayant.R3

        // Then
        assertEquals(3, priority.value)
        assertEquals("C3", priority.code)
    }

    // ===== fromValue() Tests =====

    @Test
    fun `fromValue should return R0 for value 0`() {
        // When
        val result = PrioriteTiersPayant.fromValue(0)

        // Then
        assertNotNull(result)
        assertEquals(PrioriteTiersPayant.R0, result)
    }

    @Test
    fun `fromValue should return R1 for value 1`() {
        // When
        val result = PrioriteTiersPayant.fromValue(1)

        // Then
        assertNotNull(result)
        assertEquals(PrioriteTiersPayant.R1, result)
    }

    @Test
    fun `fromValue should return R2 for value 2`() {
        // When
        val result = PrioriteTiersPayant.fromValue(2)

        // Then
        assertNotNull(result)
        assertEquals(PrioriteTiersPayant.R2, result)
    }

    @Test
    fun `fromValue should return R3 for value 3`() {
        // When
        val result = PrioriteTiersPayant.fromValue(3)

        // Then
        assertNotNull(result)
        assertEquals(PrioriteTiersPayant.R3, result)
    }

    @Test
    fun `fromValue should return null for invalid value`() {
        // When
        val result = PrioriteTiersPayant.fromValue(99)

        // Then
        assertNull(result)
    }

    @Test
    fun `fromValue should return null for negative value`() {
        // When
        val result = PrioriteTiersPayant.fromValue(-1)

        // Then
        assertNull(result)
    }

    // ===== fromCode() Tests =====

    @Test
    fun `fromCode should return R0 for code R0`() {
        // When
        val result = PrioriteTiersPayant.fromCode("R0")

        // Then
        assertNotNull(result)
        assertEquals(PrioriteTiersPayant.R0, result)
    }

    @Test
    fun `fromCode should return R1 for code C1`() {
        // When
        val result = PrioriteTiersPayant.fromCode("C1")

        // Then
        assertNotNull(result)
        assertEquals(PrioriteTiersPayant.R1, result)
    }

    @Test
    fun `fromCode should return R2 for code C2`() {
        // When
        val result = PrioriteTiersPayant.fromCode("C2")

        // Then
        assertNotNull(result)
        assertEquals(PrioriteTiersPayant.R2, result)
    }

    @Test
    fun `fromCode should return R3 for code C3`() {
        // When
        val result = PrioriteTiersPayant.fromCode("C3")

        // Then
        assertNotNull(result)
        assertEquals(PrioriteTiersPayant.R3, result)
    }

    @Test
    fun `fromCode should return null for invalid code`() {
        // When
        val result = PrioriteTiersPayant.fromCode("INVALID")

        // Then
        assertNull(result)
    }

    @Test
    fun `fromCode should return null for empty string`() {
        // When
        val result = PrioriteTiersPayant.fromCode("")

        // Then
        assertNull(result)
    }

    // ===== isPrincipal() Tests =====

    @Test
    fun `R0 isPrincipal should return true`() {
        // Given
        val priority = PrioriteTiersPayant.R0

        // Then
        assertTrue(priority.isPrincipal())
    }

    @Test
    fun `R1 isPrincipal should return false`() {
        // Given
        val priority = PrioriteTiersPayant.R1

        // Then
        assertFalse(priority.isPrincipal())
    }

    @Test
    fun `R2 isPrincipal should return false`() {
        // Given
        val priority = PrioriteTiersPayant.R2

        // Then
        assertFalse(priority.isPrincipal())
    }

    @Test
    fun `R3 isPrincipal should return false`() {
        // Given
        val priority = PrioriteTiersPayant.R3

        // Then
        assertFalse(priority.isPrincipal())
    }

    // ===== isComplementaire() Tests =====

    @Test
    fun `R0 isComplementaire should return false`() {
        // Given
        val priority = PrioriteTiersPayant.R0

        // Then
        assertFalse(priority.isComplementaire())
    }

    @Test
    fun `R1 isComplementaire should return true`() {
        // Given
        val priority = PrioriteTiersPayant.R1

        // Then
        assertTrue(priority.isComplementaire())
    }

    @Test
    fun `R2 isComplementaire should return true`() {
        // Given
        val priority = PrioriteTiersPayant.R2

        // Then
        assertTrue(priority.isComplementaire())
    }

    @Test
    fun `R3 isComplementaire should return true`() {
        // Given
        val priority = PrioriteTiersPayant.R3

        // Then
        assertTrue(priority.isComplementaire())
    }

    // ===== getDisplayLabel() Tests =====

    @Test
    fun `R0 getDisplayLabel should return R0`() {
        // Given
        val priority = PrioriteTiersPayant.R0

        // Then
        assertEquals("R0", priority.getDisplayLabel())
    }

    @Test
    fun `R1 getDisplayLabel should return C1`() {
        // Given
        val priority = PrioriteTiersPayant.R1

        // Then
        assertEquals("C1", priority.getDisplayLabel())
    }

    @Test
    fun `R2 getDisplayLabel should return C2`() {
        // Given
        val priority = PrioriteTiersPayant.R2

        // Then
        assertEquals("C2", priority.getDisplayLabel())
    }

    @Test
    fun `R3 getDisplayLabel should return C3`() {
        // Given
        val priority = PrioriteTiersPayant.R3

        // Then
        assertEquals("C3", priority.getDisplayLabel())
    }
}
