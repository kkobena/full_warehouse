package com.kobe.warehouse.service.dto.dashboard;

import java.time.LocalDate;

/**
 * DTO pour un différé à relancer (sale.differe=true, rest_to_pay>0, sale_date <= today).
 */
public record DiffereARelancerDTO(
    Long saleId,
    String clientNom,
    String clientTelephone,    // customer.phone
    Long montantDu,            // sales.rest_to_pay
    LocalDate dateEcheance,    // sales.sale_date (date de la vente différée)
    Integer joursRetard,       // CURRENT_DATE - sale_date (0=aujourd'hui, >0=retard)
    String urgence             // "CRITIQUE" | "AUJOURD_HUI" | "RETARD"
) {}

