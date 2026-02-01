package com.kobe.warehouse.sales.data.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for TiersPayant data model
 * Tests display methods and helper functions
 */
class TiersPayantTest {

    // ===== getDisplayName() Tests =====

    @Test
    fun `getDisplayName should return fullName if available`() {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "MUGEF",
            fullName = "Mutuelle Générale des Fonctionnaires"
        )

        // Then
        assertEquals("Mutuelle Générale des Fonctionnaires", tiersPayant.getDisplayName())
    }

    @Test
    fun `getDisplayName should return name if fullName is null`() {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "MUGEF",
            fullName = null
        )

        // Then
        assertEquals("MUGEF", tiersPayant.getDisplayName())
    }

    @Test
    fun `getDisplayName should return default format if both name and fullName are null`() {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = null,
            fullName = null
        )

        // Then
        assertEquals("Tiers payant #1", tiersPayant.getDisplayName())
    }

    // ===== isEnabled() Tests =====

    @Test
    fun `isEnabled should return true if statut is not DISABLE`() {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "MUGEF",
            statut = "ENABLE"
        )

        // Then
        assertTrue(tiersPayant.isEnabled())
    }

    @Test
    fun `isEnabled should return true if statut is null`() {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "MUGEF",
            statut = null
        )

        // Then
        assertTrue(tiersPayant.isEnabled())
    }

    @Test
    fun `isEnabled should return false if statut is DISABLE`() {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "MUGEF",
            statut = "DISABLE"
        )

        // Then
        assertFalse(tiersPayant.isEnabled())
    }

    // ===== getCategoryLabel() Tests =====

    @Test
    fun `getCategoryLabel should return Assurance for ASSURANCE`() {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "MUGEF",
            categorie = "ASSURANCE"
        )

        // Then
        assertEquals("Assurance", tiersPayant.getCategoryLabel())
    }

    @Test
    fun `getCategoryLabel should return Carnet for CARNET`() {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "CARNET-CI",
            categorie = "CARNET"
        )

        // Then
        assertEquals("Carnet", tiersPayant.getCategoryLabel())
    }

    @Test
    fun `getCategoryLabel should return Non défini for null category`() {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "TEST",
            categorie = null
        )

        // Then
        assertEquals("Non défini", tiersPayant.getCategoryLabel())
    }

    @Test
    fun `getCategoryLabel should return category value for unknown category`() {
        // Given
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "TEST",
            categorie = "OTHER"
        )

        // Then
        assertEquals("OTHER", tiersPayant.getCategoryLabel())
    }

    // ===== Complete Object Tests =====

    @Test
    fun `TiersPayant with all fields should be created correctly`() {
        // Given & When
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "MUGEF",
            fullName = "Mutuelle Générale des Fonctionnaires",
            codeOrganisme = "ORG001",
            telephone = "0123456789",
            email = "contact@mugef.ci",
            categorie = "ASSURANCE",
            statut = "ENABLE",
            plafondConso = 100000,
            plafondAbsolu = true,
            nbreBordereaux = 5
        )

        // Then
        assertEquals(1L, tiersPayant.id)
        assertEquals("MUGEF", tiersPayant.name)
        assertEquals("Mutuelle Générale des Fonctionnaires", tiersPayant.fullName)
        assertEquals("ORG001", tiersPayant.codeOrganisme)
        assertEquals("0123456789", tiersPayant.telephone)
        assertEquals("contact@mugef.ci", tiersPayant.email)
        assertEquals("ASSURANCE", tiersPayant.categorie)
        assertEquals("ENABLE", tiersPayant.statut)
        assertEquals(100000, tiersPayant.plafondConso)
        assertEquals(true, tiersPayant.plafondAbsolu)
        assertEquals(5, tiersPayant.nbreBordereaux)
    }

    @Test
    fun `TiersPayant with minimal fields should be created correctly`() {
        // Given & When
        val tiersPayant = TiersPayant(
            id = 1L,
            name = "MUGEF"
        )

        // Then
        assertEquals(1L, tiersPayant.id)
        assertEquals("MUGEF", tiersPayant.name)
    }
}
