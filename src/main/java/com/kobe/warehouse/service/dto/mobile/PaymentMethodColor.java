package com.kobe.warehouse.service.dto.mobile;

import com.kobe.warehouse.domain.enumeration.PaymentGroup;

/**
 * Payment method color enumeration for mobile reports.
 * Maps payment codes/groups to display colors.
 */
public enum PaymentMethodColor {
    CASH("CASH", "#28A745"),      // Green
    CB("CB", "#007BFF"),          // Blue
    MOBILE("MOBILE", "#FFC107"),  // Yellow
    VIREMENT("VIREMENT", "#17A2B8"), // Cyan
    CHEQUE("CHEQUE", "#6C757D"),  // Gray
    CREDIT("CREDIT", "#DC3545"),  // Red
    CAUTION("CAUTION", "#6F42C1"), // Purple
    DEFAULT("DEFAULT", "#6C757D"); // Gray (fallback)

    private final String code;
    private final String color;

    PaymentMethodColor(String code, String color) {
        this.code = code;
        this.color = color;
    }

    public String getCode() {
        return code;
    }

    public String getColor() {
        return color;
    }

    /**
     * Get color for a payment code.
     *
     * @param paymentCode Payment mode code
     * @return Color hex code
     */
    public static String getColorForCode(String paymentCode) {
        if (paymentCode == null) {
            return DEFAULT.color;
        }
        for (PaymentMethodColor pmc : values()) {
            if (pmc.code.equalsIgnoreCase(paymentCode)) {
                return pmc.color;
            }
        }
        return DEFAULT.color;
    }

    /**
     * Get color for a PaymentGroup.
     *
     * @param group PaymentGroup enum
     * @return Color hex code
     */
    public static String getColorForGroup(PaymentGroup group) {
        if (group == null) {
            return DEFAULT.color;
        }
        return switch (group) {
            case CASH -> CASH.color;
            case CB -> CB.color;
            case MOBILE -> MOBILE.color;
            case VIREMENT -> VIREMENT.color;
            case CHEQUE -> CHEQUE.color;
            case CREDIT -> CREDIT.color;
            case CAUTION -> CAUTION.color;
        };
    }
}
