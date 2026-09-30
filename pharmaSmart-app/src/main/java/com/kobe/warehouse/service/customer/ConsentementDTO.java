package com.kobe.warehouse.service.customer;

import com.kobe.warehouse.domain.enumeration.CanalConsentement;
import java.time.LocalDateTime;

/**
 * État d'un canal pour un client, ou l'un de ses changements.
 *
 * @param accorde null tant que le client n'a jamais été interrogé sur ce canal
 */
public record ConsentementDTO(CanalConsentement canal, Boolean accorde, LocalDateTime date, String recueilliPar) {}
