package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.Periodicite;
import java.util.List;

/**
 * Corps de la requête PATCH mass-update-facture-config.
 * Tous les champs optionnels sauf {@code ids}.
 */
public record MassUpdateFactureConfigRequest(
    List<Integer> ids,
    Boolean inclureAutoDefinitif,
    Boolean inclureAutoProvisoire,
    Periodicite periodiciteDefinitive,
    Periodicite periodiciteProvisoire
) {}
