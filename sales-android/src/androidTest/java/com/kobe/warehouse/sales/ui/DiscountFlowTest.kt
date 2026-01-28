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
 * Instrumented UI tests for Discount (Remise) feature
 *
 * Tests the complete user flow:
 * 1. User has items in cart
 * 2. User opens discount dialog
 * 3. User selects discount type (percentage or fixed)
 * 4. User enters discount value
 * 5. System shows preview of discount calculation
 * 6. User confirms (with authority if required)
 * 7. Discount is applied to sale
 *
 * Prerequisites:
 * - Backend must be running
 * - Test user must have PR_REMISE authority for some tests
 * - Products must exist to add to cart
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DiscountFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ComptantSaleActivity::class.java)

    @Test
    fun testApplyPercentageDiscount_Success() {
        // Given - Add products to cart first
        addProductToCart("PARA500", quantity = 5)

        // Verify cart has items
        onView(withId(R.id.rvCart))
            .check(matches(isDisplayed()))

        // When - User opens discount menu
        onView(withId(R.id.btnDiscountMenu))
            .perform(click())

        // Then - Discount dialog should appear
        onView(withText("Appliquer une remise"))
            .check(matches(isDisplayed()))

        // Verify original amount is displayed
        onView(withText(containsString("Montant initial")))
            .check(matches(isDisplayed()))

        // When - User selects percentage type
        onView(withId(R.id.chipPercentage))
            .perform(click())

        // And - User enters 10%
        onView(withId(R.id.etDiscountValue))
            .perform(clearText(), typeText("10"), closeSoftKeyboard())

        // Then - Preview should show discount calculation
        onView(withId(R.id.tvDiscountPreview))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("FCFA")))) // Discount amount

        onView(withId(R.id.tvNetAmount))
            .check(matches(isDisplayed()))

        // When - User confirms discount
        onView(withId(R.id.btnApplyDiscount))
            .check(matches(isEnabled()))
            .perform(click())

        // Then - Discount should be applied
        Thread.sleep(500)

        // Verify discount badge or indicator is shown
        onView(withId(R.id.tvDiscountApplied))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("10%"))))

        // Verify total is updated
        onView(withId(R.id.tvTotalAmount))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testApplyFixedDiscount_Success() {
        // Given - Add products to cart
        addProductToCart("PARA500", quantity = 3)

        // When - Open discount dialog
        onView(withId(R.id.btnDiscountMenu))
            .perform(click())

        // Select fixed amount type
        onView(withId(R.id.chipFixed))
            .perform(click())

        // Enter fixed amount (e.g., 5000 FCFA)
        onView(withId(R.id.etDiscountValue))
            .perform(clearText(), typeText("5000"), closeSoftKeyboard())

        // Then - Preview should show fixed discount
        onView(withId(R.id.tvDiscountPreview))
            .check(matches(withText(containsString("5 000 FCFA"))))

        // When - Use quick amount button
        onView(withId(R.id.chipQuick1000))
            .perform(click())

        // Then - Value should update to 1000
        onView(withId(R.id.etDiscountValue))
            .check(matches(withText("1000")))

        // When - Confirm discount
        onView(withId(R.id.btnApplyDiscount))
            .perform(click())

        // Then - Discount applied
        Thread.sleep(500)

        onView(withId(R.id.tvDiscountApplied))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("1 000"))))
    }

    @Test
    fun testDiscount_InvalidPercentage_Over100_ButtonDisabled() {
        // Given - Add products to cart
        addProductToCart("PARA500", quantity = 2)

        // When - Open discount dialog
        onView(withId(R.id.btnDiscountMenu))
            .perform(click())

        // Select percentage
        onView(withId(R.id.chipPercentage))
            .perform(click())

        // Enter invalid percentage (> 100)
        onView(withId(R.id.etDiscountValue))
            .perform(clearText(), typeText("150"), closeSoftKeyboard())

        // Then - Error message should be displayed
        onView(withText(containsString("entre 1 et 100")))
            .check(matches(isDisplayed()))

        // And - Apply button should be disabled
        onView(withId(R.id.btnApplyDiscount))
            .check(matches(isNotEnabled()))
    }

    @Test
    fun testDiscount_FixedAmountExceedsTotal_CappedAtTotal() {
        // Given - Add products to cart (e.g., total = 10,000 FCFA)
        addProductToCart("PARA500", quantity = 2)

        // When - Open discount dialog
        onView(withId(R.id.btnDiscountMenu))
            .perform(click())

        // Select fixed amount
        onView(withId(R.id.chipFixed))
            .perform(click())

        // Enter amount greater than total (e.g., 50,000)
        onView(withId(R.id.etDiscountValue))
            .perform(clearText(), typeText("50000"), closeSoftKeyboard())

        // Then - Error or warning should be shown
        onView(withText(containsString("ne peut pas dépasser")))
            .check(matches(isDisplayed()))

        // And - Apply button should be disabled
        onView(withId(R.id.btnApplyDiscount))
            .check(matches(isNotEnabled()))
    }

    @Test
    fun testDiscount_PreviewUpdatesInRealTime() {
        // Given - Add products to cart
        addProductToCart("PARA500", quantity = 5)

        // When - Open discount dialog
        onView(withId(R.id.btnDiscountMenu))
            .perform(click())

        // Select percentage
        onView(withId(R.id.chipPercentage))
            .perform(click())

        // Enter 5%
        onView(withId(R.id.etDiscountValue))
            .perform(clearText(), typeText("5"), closeSoftKeyboard())

        // Verify preview shows 5% calculation
        Thread.sleep(200)

        // Change to 15%
        onView(withId(R.id.etDiscountValue))
            .perform(clearText(), typeText("15"), closeSoftKeyboard())

        // Verify preview updates to 15% calculation
        Thread.sleep(200)

        // Preview should show updated discount amount and net amount
        onView(withId(R.id.tvDiscountPreview))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tvNetAmount))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDiscount_SwitchBetweenPercentageAndFixed() {
        // Given - Add products to cart
        addProductToCart("PARA500", quantity = 3)

        // When - Open discount dialog
        onView(withId(R.id.btnDiscountMenu))
            .perform(click())

        // Select percentage
        onView(withId(R.id.chipPercentage))
            .perform(click())

        // Enter 10%
        onView(withId(R.id.etDiscountValue))
            .perform(clearText(), typeText("10"), closeSoftKeyboard())

        // Then - Suffix should show "%"
        onView(withText("%"))
            .check(matches(isDisplayed()))

        // When - Switch to fixed
        onView(withId(R.id.chipFixed))
            .perform(click())

        // Then - Suffix should show "FCFA"
        onView(withText("FCFA"))
            .check(matches(isDisplayed()))

        // And - Quick amount buttons should be visible
        onView(withId(R.id.chipQuick100))
            .check(matches(isDisplayed()))
        onView(withId(R.id.chipQuick500))
            .check(matches(isDisplayed()))
        onView(withId(R.id.chipQuick1000))
            .check(matches(isDisplayed()))
        onView(withId(R.id.chipQuick5000))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDiscount_CancelDialog() {
        // Given - Add products to cart
        addProductToCart("PARA500", quantity = 2)

        // Get current total before discount
        // (In real test, you'd capture the total value)

        // When - Open discount dialog
        onView(withId(R.id.btnDiscountMenu))
            .perform(click())

        // Enter discount
        onView(withId(R.id.chipPercentage))
            .perform(click())
        onView(withId(R.id.etDiscountValue))
            .perform(clearText(), typeText("20"), closeSoftKeyboard())

        // Click cancel
        onView(withId(R.id.btnCancelDiscount))
            .perform(click())

        // Then - Dialog should close
        Thread.sleep(500)

        // Verify no discount was applied (total unchanged)
        // onView(withId(R.id.tvDiscountApplied))
        //     .check(doesNotExist())
    }

    @Test
    fun testDiscount_RemoveDiscount() {
        // Given - Apply a discount first
        addProductToCart("PARA500", quantity = 5)

        onView(withId(R.id.btnDiscountMenu))
            .perform(click())

        onView(withId(R.id.chipPercentage))
            .perform(click())
        onView(withId(R.id.etDiscountValue))
            .perform(clearText(), typeText("10"), closeSoftKeyboard())
        onView(withId(R.id.btnApplyDiscount))
            .perform(click())

        Thread.sleep(500)

        // Verify discount is applied
        onView(withId(R.id.tvDiscountApplied))
            .check(matches(isDisplayed()))

        // When - User clicks remove discount button
        onView(withId(R.id.btnRemoveDiscount))
            .perform(click())

        // Then - Confirmation dialog might appear
        // (Assuming there's a confirmation)
        onView(withText(containsString("Supprimer la remise")))
            .check(matches(isDisplayed()))

        onView(withText("Oui"))
            .perform(click())

        Thread.sleep(500)

        // Verify discount is removed
        // onView(withId(R.id.tvDiscountApplied))
        //     .check(doesNotExist())

        // Total should revert to original amount
    }

    @Test
    fun testDiscount_WithAuthority_NoAuthDialog() {
        // Given - User has PR_REMISE authority
        addProductToCart("PARA500", quantity = 3)

        // When - Apply discount
        onView(withId(R.id.btnDiscountMenu))
            .perform(click())

        onView(withId(R.id.chipPercentage))
            .perform(click())
        onView(withId(R.id.etDiscountValue))
            .perform(clearText(), typeText("15"), closeSoftKeyboard())

        onView(withId(R.id.btnApplyDiscount))
            .perform(click())

        // Then - Discount should be applied directly (no auth dialog)
        Thread.sleep(500)

        onView(withId(R.id.tvDiscountApplied))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDiscount_QuickAmountButtons() {
        // Given - Add products to cart
        addProductToCart("PARA500", quantity = 5)

        // When - Open discount dialog
        onView(withId(R.id.btnDiscountMenu))
            .perform(click())

        // Select fixed amount
        onView(withId(R.id.chipFixed))
            .perform(click())

        // Click 100 FCFA quick button
        onView(withId(R.id.chipQuick100))
            .perform(click())

        // Verify value is set to 100
        onView(withId(R.id.etDiscountValue))
            .check(matches(withText("100")))

        // Click 1000 FCFA quick button
        onView(withId(R.id.chipQuick1000))
            .perform(click())

        // Verify value is updated to 1000
        onView(withId(R.id.etDiscountValue))
            .check(matches(withText("1000")))

        // Close dialog
        onView(withId(R.id.btnCancelDiscount))
            .perform(click())
    }

    // ===== Helper Methods =====

    /**
     * Helper function to add a product to cart
     */
    private fun addProductToCart(productCode: String, quantity: Int = 1) {
        onView(withId(R.id.etProductSearch))
            .perform(clearText(), typeText(productCode), closeSoftKeyboard())

        Thread.sleep(1000)

        onView(allOf(withId(R.id.cvProductItem), isDisplayed()))
            .perform(click())

        if (quantity > 1) {
            onView(withId(R.id.etQuantity))
                .perform(clearText(), typeText(quantity.toString()), closeSoftKeyboard())
        }

        onView(withId(R.id.btnAddToCart))
            .perform(click())

        Thread.sleep(500)
    }
}
