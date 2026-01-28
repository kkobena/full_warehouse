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
 * Instrumented UI tests for Déconditionnement feature
 *
 * Tests the complete user flow:
 * 1. User adds a product to cart
 * 2. System detects product is deconditionnable with low detail stock
 * 3. Déconditionnement dialog appears
 * 4. User confirms déconditionnement
 * 5. Product is added with correct quantity (box broken into units)
 *
 * Prerequisites:
 * - Backend must be running
 * - Test user must be logged in
 * - Test product must exist with deconditionnable=true
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DeconditionnementFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ComptantSaleActivity::class.java)

    @Test
    fun testManualDeconditionnement_Success() {
        // Given - Product search field is visible
        onView(withId(R.id.etProductSearch))
            .check(matches(isDisplayed()))

        // When - User searches for a deconditionnable product
        onView(withId(R.id.etProductSearch))
            .perform(typeText("PARA500"), closeSoftKeyboard())

        // Wait for search results
        Thread.sleep(1000)

        // When - User clicks on a product
        onView(withId(R.id.rvProductList))
            .perform(scrollToPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0))
        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        // Then - Déconditionnement dialog should appear
        onView(withText("Déconditionnement"))
            .check(matches(isDisplayed()))

        // Verify dialog shows stock information
        onView(withText(containsString("Stock boîte")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("Stock détail")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("Unités par boîte")))
            .check(matches(isDisplayed()))

        // When - User enters quantity to décondition (e.g., 2 boxes)
        onView(withId(R.id.etDeconditionQuantity))
            .perform(clearText(), typeText("2"), closeSoftKeyboard())

        // Verify result preview is shown
        onView(withText(containsString("boîte(s)")))
            .check(matches(isDisplayed()))
        onView(withText(containsString("unité(s)")))
            .check(matches(isDisplayed()))

        // When - User confirms déconditionnement
        onView(withId(R.id.btnDeconditionConfirm))
            .perform(click())

        // Then - Dialog should close
        Thread.sleep(500)

        // Verify product was added to cart with correct quantity
        onView(withId(R.id.rvCart))
            .check(matches(isDisplayed()))

        // Verify success message or toast (if implemented)
        // onView(withText(containsString("Déconditionnement effectué")))
        //     .inRoot(withDecorView(not(activityRule.activity.window.decorView)))
        //     .check(matches(isDisplayed()))
    }

    @Test
    fun testDeconditionnement_InsufficientStock_ShowsError() {
        // Given - Product search
        onView(withId(R.id.etProductSearch))
            .perform(typeText("PARA500"), closeSoftKeyboard())

        Thread.sleep(1000)

        // When - User clicks product
        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        // Then - Déconditionnement dialog appears
        onView(withText("Déconditionnement"))
            .check(matches(isDisplayed()))

        // When - User enters quantity greater than available boxes
        onView(withId(R.id.etDeconditionQuantity))
            .perform(clearText(), typeText("999"), closeSoftKeyboard())

        // Then - Error message should be displayed
        onView(withText(containsString("Stock insuffisant")))
            .check(matches(isDisplayed()))

        // And - Confirm button should be disabled
        onView(withId(R.id.btnDeconditionConfirm))
            .check(matches(isNotEnabled()))
    }

    @Test
    fun testDeconditionnement_CancelDialog() {
        // Given - Open déconditionnement dialog
        onView(withId(R.id.etProductSearch))
            .perform(typeText("PARA500"), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        // When - User clicks cancel
        onView(withId(R.id.btnDeconditionCancel))
            .perform(click())

        // Then - Dialog should close without adding product
        Thread.sleep(500)

        // Verify cart is empty or unchanged
        onView(withId(R.id.tvCartEmpty))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAutoDeconditionnement_BelowThreshold() {
        // Given - Product with low detail stock (below seuilDeconditionnement)
        // This test assumes auto-déconditionnement is triggered automatically

        onView(withId(R.id.etProductSearch))
            .perform(typeText("AUTO_DECOND"), closeSoftKeyboard())

        Thread.sleep(1000)

        // When - User selects product with auto-decondition threshold
        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        // Then - Déconditionnement should happen automatically
        // OR confirmation dialog appears with pre-filled quantity
        onView(withText("Déconditionnement"))
            .check(matches(isDisplayed()))

        // Verify quantity is pre-filled (e.g., 1 box)
        onView(withId(R.id.etDeconditionQuantity))
            .check(matches(withText("1")))

        // User can confirm
        onView(withId(R.id.btnDeconditionConfirm))
            .perform(click())

        Thread.sleep(500)

        // Verify product added to cart
        onView(withId(R.id.rvCart))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDeconditionnement_StockInfoDisplay() {
        // Given - Open déconditionnement dialog
        onView(withId(R.id.etProductSearch))
            .perform(typeText("PARA500"), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        // Then - Verify all stock information is displayed correctly
        onView(withId(R.id.tvBoxStock))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("boîte"))))

        onView(withId(R.id.tvDetailStock))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("détail"))))

        onView(withId(R.id.tvItemQty))
            .check(matches(isDisplayed()))

        // Verify explanation text is shown
        onView(withText(containsString("déconditionnement permet")))
            .check(matches(isDisplayed()))

        // Close dialog
        onView(withId(R.id.btnDeconditionCancel))
            .perform(click())
    }

    @Test
    fun testDeconditionnement_ResultPreviewUpdates() {
        // Given - Open déconditionnement dialog
        onView(withId(R.id.etProductSearch))
            .perform(typeText("PARA500"), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        // When - User enters 1 box
        onView(withId(R.id.etDeconditionQuantity))
            .perform(clearText(), typeText("1"), closeSoftKeyboard())

        // Then - Result should show "1 boîte → X unités" (where X = itemQty)
        onView(withId(R.id.tvDeconditionResult))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("1 boîte"))))

        // When - User changes to 3 boxes
        onView(withId(R.id.etDeconditionQuantity))
            .perform(clearText(), typeText("3"), closeSoftKeyboard())

        // Then - Result should update to "3 boîtes → 3X unités"
        onView(withId(R.id.tvDeconditionResult))
            .check(matches(withText(containsString("3 boîte"))))

        // Close dialog
        onView(withId(R.id.btnDeconditionCancel))
            .perform(click())
    }

    // Helper function to scroll RecyclerView to position
    private inline fun <reified VH : androidx.recyclerview.widget.RecyclerView.ViewHolder> scrollToPosition(
        position: Int
    ): androidx.test.espresso.ViewAction {
        return androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition<VH>(position)
    }
}
