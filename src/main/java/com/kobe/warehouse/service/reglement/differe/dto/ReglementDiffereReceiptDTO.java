package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.service.utils.NumberUtil;

public record ReglementDiffereReceiptDTO(
    String userFirstName,
    String userLastName,
    String firstName,
    String lastName,
    int expectedAmount,
    int montantVerse,
    int paidAmount,
    String mode,
    String libelleMode,
    int solde
) {
    public String userfullName() {
        return String.format("%s.%s", userFirstName.charAt(0), userLastName);
    }
    public String customerfullName() {
        return String.format("%s %s", firstName, lastName);
    }
    public String formattedPaidAmount() {
        return NumberUtil.formatToString(paidAmount);
    }
    public String formattedMontantVerse() {
        return NumberUtil.formatToString(montantVerse);
    }
    public String formattedSolde() {
        return NumberUtil.formatToString(solde);
    }
    public String formattedExpectedAmount() {
        return NumberUtil.formatToString(expectedAmount);
    }
    public String monnaie() {
        if (ModePaimentCode.CASH.name().equals(mode) && montantVerse > expectedAmount) {
            return NumberUtil.formatToString(montantVerse - expectedAmount);
        }
        return "";
    }
}
