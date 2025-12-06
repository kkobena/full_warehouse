package com.kobe.warehouse.service.dto.report;

import java.time.LocalDate;

public record TiersPayantInvoiceDTO(
    Long factureId,
    String numeroFacture,
    LocalDate dateFacture,
    String tiersPayantLibelle,
    String groupeTiersPayantLibelle,
    Integer montantFacture,
    Integer montantPaye,
    Integer montantRestant,
    InvoiceStatus statut,
    Integer daysSinceInvoice,
    AgeCategory ageCategory
) {
    public enum InvoiceStatus {
        PAID,
        UNPAID,
        PARTIAL
    }

    public enum AgeCategory {
        LESS_THAN_30,      // < 30 days
        BETWEEN_30_60,     // 30-60 days
        BETWEEN_60_90,     // 60-90 days
        MORE_THAN_90       // > 90 days
    }
}
