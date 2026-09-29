package com.kobe.warehouse.service.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Demande de délivrance malgré une alerte santé. Sans le droit {@code pr-forcer-alerte-sante},
 * l'utilisateur fournit la clé de sécurité d'un collègue qui le détient.
 */
public record DerogationAlerteSanteDTO(
    @NotNull Integer produitId,
    @NotBlank @Size(max = 255) String motif,
    String actionAuthorityKey
) {}
