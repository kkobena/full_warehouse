package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Délai moyen, en jours, entre la commande et la réception, par fournisseur (projection). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DelaiFournisseurDTO(String cle, Double delaiMoyen, Long nombre) {}
