package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.service.utils.DateUtil;
import com.kobe.warehouse.service.utils.NumberUtil;

import java.time.LocalDateTime;

public record ReglementDiffereDTO(Long id, String firstName, String lastName, LocalDateTime mvtDate,
                                  int expectedAmount, int montantVerse, int paidAmount, String mode,
                                  String libelleMode) {
    public String formattedPaidAmount() {
        return NumberUtil.formatToString(paidAmount);
    }

    public String formattedMontantVerse() {
        return NumberUtil.formatToString(montantVerse);
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

    public String user() {
        return String.format("%s.%s", firstName.charAt(0), lastName);
    }

    public String dateReglement() {
        return DateUtil.format(mvtDate);
    }

    public String solde() {
        int solde = expectedAmount - paidAmount;
        return NumberUtil.formatToString(solde >= 0 ? solde : 0);
    }
}
