package com.kobe.warehouse.service.dto.controle;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Prise en compte des alertes affichées pour ce panier. Le motif est exigé dès qu'une alerte est une
 * contre-indication ; sans le droit {@code pr-forcer-alerte-sante}, la clé d'un collègue qui le détient.
 */
public record PriseEnCompteControleDTO(
    @NotEmpty List<Integer> produitIds,
    @Size(max = 255) String motif,
    String actionAuthorityKey
) {}
