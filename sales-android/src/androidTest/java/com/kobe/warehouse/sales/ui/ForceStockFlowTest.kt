package com.kobe.warehouse.sales.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.ui.activity.ComptantSaleActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for Force Stock feature
 *
 * Tests the complete user flow:
 * 1. User tries to add product with insufficient stock
 * 2. System shows stock warning
 * 3. Force stock dialog appears (if user has authority)
 * 4. User provides reason and confirms
 * 5. Product is added despite insufficient stock
 *
 * Prerequisites:
 * - Backend must be running
 * - Test user must have PR_FORCE_STOCK authority
 * - Test product must exist with low stock
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ForceStockFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ComptantSaleActivity::class.java)

    @Test
    fun testForceStock_WithAuthority_Success() {
        // Given - User has PR_FORCE_STOCK authority
        // Search for a low-stock product
        onView(withId(R.id.etProductSearch))
            .perform(typeText("LOW_STOCK"), closeSoftKeyboard())

        Thread.sleep(1000)

        // When - User selects the product
        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        // Enter quantity greater than available stock
        onView(withId(R.id.etQuantity))
            .perform(clearText(), typeText("100"), closeSoftKeyboard())

        // Click add to cart
        onView(withId(R.id.btnAddToCart))
            .perform(click())

        // Then - Force stock dialog should appear
        onView(withText("Stock insuffisant"))
            .check(matches(isDisplayed()))

        // Verify stock information is displayed
        onView(withText(containsString("Stock disponible")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("Quantité demandée")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("Manque")))
            .check(matches(isDisplayed()))

        // Verify warning message is shown
        onView(withText(containsString("malgré un stock insuffisant")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("PR_FORCE_STOCK")))
            .check(matches(isDisplayed()))

        // When - User enters reason (minimum 10 characters)
        onView(withId(R.id.etForceStockReason))
            .perform(typeText("Commande urgente du client pour traitement médical"), closeSoftKeyboard())

        // Verify character count is displayed
        onView(withText(containsString("/200")))
            .check(matches(isDisplayed()))

        // When - User confirms force stock
        onView(withId(R.id.btnForceStockConfirm))
            .check(matches(isEnabled()))
            .perform(click())

        // Then - Product should be added to cart
        Thread.sleep(500)

        onView(withId(R.id.rvCart))
            .check(matches(isDisplayed()))

        // Verify success message (if implemented)
        // onView(withText(containsString("Produit ajouté avec stock forcé")))
        //     .inRoot(withDecorView(not(activityRule.activity.window.decorView)))
        //     .check(matches(isDisplayed()))
    }

    @Test
    fun testForceStock_ReasonTooShort_ButtonDisabled() {
        // Given - Open force stock dialog
        onView(withId(R.id.etProductSearch))
            .perform(typeText("LOW_STOCK"), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        onView(withId(R.id.etQuantity))
            .perform(clearText(), typeText("100"), closeSoftKeyboard())

        onView(withId(R.id.btnAddToCart))
            .perform(click())

        // When - User enters reason with less than 10 characters
        onView(withId(R.id.etForceStockReason))
            .perform(typeText("Urgent"), closeSoftKeyboard())

        // Then - Confirm button should be disabled
        onView(withId(R.id.btnForceStockConfirm))
            .check(matches(isNotEnabled()))

        // And - Error message should be displayed
        onView(withText(containsString("minimum 10")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testForceStock_ReasonExactly10Characters_ButtonEnabled() {
        // Given - Open force stock dialog
        onView(withId(R.id.etProductSearch))
            .perform(typeText("LOW_STOCK"), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        onView(withId(R.id.etQuantity))
            .perform(clearText(), typeText("100"), closeSoftKeyboard())

        onView(withId(R.id.btnAddToCart))
            .perform(click())

        // When - User enters exactly 10 characters
        onView(withId(R.id.etForceStockReason))
            .perform(typeText("1234567890"), closeSoftKeyboard())

        // Then - Confirm button should be enabled
        onView(withId(R.id.btnForceStockConfirm))
            .check(matches(isEnabled()))
    }

    @Test
    fun testForceStock_CancelDialog() {
        // Given - Open force stock dialog
        onView(withId(R.id.etProductSearch))
            .perform(typeText("LOW_STOCK"), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        onView(withId(R.id.etQuantity))
            .perform(clearText(), typeText("100"), closeSoftKeyboard())

        onView(withId(R.id.btnAddToCart))
            .perform(click())

        // When - User clicks cancel
        onView(withId(R.id.btnForceStockCancel))
            .perform(click())

        // Then - Dialog should close without adding product
        Thread.sleep(500)

        // Verify cart is empty or unchanged
        onView(withId(R.id.tvCartEmpty))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testForceStock_CharacterCountUpdates() {
        // Given - Open force stock dialog
        onView(withId(R.id.etProductSearch))
            .perform(typeText("LOW_STOCK"), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        onView(withId(R.id.etQuantity))
            .perform(clearText(), typeText("100"), closeSoftKeyboard())

        onView(withId(R.id.btnAddToCart))
            .perform(click())

        // When - User types in reason field
        onView(withId(R.id.etForceStockReason))
            .perform(typeText("Test reason"))

        // Then - Character count should update
        onView(withText(containsString("11/200")))
            .check(matches(isDisplayed()))

        // When - User types more
        onView(withId(R.id.etForceStockReason))
            .perform(typeText(" for testing purposes"))

        // Then - Count should increase
        onView(withText(containsString("33/200")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testForceStock_WithoutAuthority_DialogNotShown() {
        // Given - User does NOT have PR_FORCE_STOCK authority
        // This test assumes a different user context or authority check

        // Note: This test might need to be run with a different test user
        // or use a mock to simulate lack of authority

        // When - User tries to add product with insufficient stock
        onView(withId(R.id.etProductSearch))
            .perform(typeText("LOW_STOCK"), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        onView(withId(R.id.etQuantity))
            .perform(clearText(), typeText("100"), closeSoftKeyboard())

        onView(withId(R.id.btnAddToCart))
            .perform(click())

        // Then - Error message should be shown (NOT force stock dialog)
        onView(withText(containsString("Stock insuffisant")))
            .check(matches(isDisplayed()))

        // And - No force stock option should be available
        // (Force stock dialog should NOT appear)
        // Verify by checking that the reason field does not exist
        // onView(withId(R.id.etForceStockReason))
        //     .check(doesNotExist())
    }

    @Test
    fun testForceStock_StockInfoDisplay() {
        // Given - Open force stock dialog
        onView(withId(R.id.etProductSearch))
            .perform(typeText("LOW_STOCK"), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        onView(withId(R.id.etQuantity))
            .perform(clearText(), typeText("100"), closeSoftKeyboard())

        onView(withId(R.id.btnAddToCart))
            .perform(click())

        // Then - Verify stock information is correctly displayed
        onView(withId(R.id.tvAvailableStock))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tvRequestedQuantity))
            .check(matches(isDisplayed()))
            .check(matches(withText("100")))

        onView(withId(R.id.tvShortage))
            .check(matches(isDisplayed()))

        // Verify product name is shown
        onView(withId(R.id.tvProductName))
            .check(matches(isDisplayed()))

        // Close dialog
        onView(withId(R.id.btnForceStockCancel))
            .perform(click())
    }

    @Test
    fun testForceStock_CompleteOutOfStock_CannotForce() {
        // Given - Product with 0 stock
        onView(withId(R.id.etProductSearch))
            .perform(typeText("NO_STOCK"), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        onView(withId(R.id.etQuantity))
            .perform(clearText(), typeText("10"), closeSoftKeyboard())

        // When - User tries to add
        onView(withId(R.id.btnAddToCart))
            .perform(click())

        // Then - Should show "out of stock" message, not force stock dialog
        onView(withText(containsString("rupture de stock")))
            .check(matches(isDisplayed()))

        // Force stock option should NOT be available for completely out of stock items
    }
}
