package com.kobe.warehouse.service.dto.ordonnance;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import java.time.LocalDate;

/** Vente rattachée à une ordonnance ; {@code annulee} prime sur {@code statut}. */
public record VenteLieeDTO(Long salesId, LocalDate salesDate, SalesStatut statut, boolean annulee) {}
