package com.kobe.warehouse.service.dto.mobile;

import java.util.List;

/**
 * DTO for cashier-specific recap data in cash summary report.
 *
 * @param userId       User ID of the cashier
 * @param userName     Display name of the cashier
 * @param userInitials Initials for compact display (e.g., "J.D")
 * @param details      Detailed line items for this cashier
 * @param summary      Summary items (e.g., totals) for this cashier
 */
public record CashierRecapDTO(
    long userId,
    String userName,
    String userInitials,
    List<SummaryItemDTO> details,
    List<SummaryItemDTO> summary
) {
    /**
     * Creates a CashierRecapDTO with auto-generated initials.
     */
    public static CashierRecapDTO of(long userId, String userName, List<SummaryItemDTO> details, List<SummaryItemDTO> summary) {
        String initials = generateInitials(userName);
        return new CashierRecapDTO(userId, userName, initials, details, summary);
    }

    private static String generateInitials(String name) {
        if (name == null || name.isBlank()) {
            return "??";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].charAt(0) + "." + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
