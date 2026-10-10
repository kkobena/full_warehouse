package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * Une facture non soldée : son organisme, sa date, ce qui reste dû, et le délai contractuel du groupe de l'organisme (projection).
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record FactureEncoursDTO(String cle, String libelle, LocalDate invoiceDate, Long reste, Integer delaiGroupe) {}
