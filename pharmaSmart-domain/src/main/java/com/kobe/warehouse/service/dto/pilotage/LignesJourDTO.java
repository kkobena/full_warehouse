package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** Lignes de vente d'un jour, CA de l'officine : base de la marge et du panier en articles. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LignesJourDTO(LocalDate jour, long caTtc, long caHt, long coutHt, long quantiteServie) {}
