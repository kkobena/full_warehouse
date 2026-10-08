package com.kobe.warehouse.service.dto.ordonnance;

import com.kobe.warehouse.domain.enumeration.StatutOrdonnance;
import java.time.LocalDate;
import java.util.List;

/** Ordonnance et son suivi. */
public record OrdonnanceDTO(
    Integer id,
    Integer customerId,
    Integer prescripteurId,
    String prescripteurNom,
    LocalDate datePrescription,
    String source,
    int renouvellements,
    int renouvellementsRestants,
    LocalDate dateFinValidite,
    StatutOrdonnance statut,
    boolean cloturee,
    String note,
    List<OrdonnanceLigneDTO> lignes,
    List<VenteLieeDTO> ventes
) {}
