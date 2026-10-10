package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Clients actifs (identifiés) d'une période, CA total et CA réalisé avec un client identifié (projection). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ClienteleDTO(Long actifs, Long caTotal, Long caIdentifie) {}
