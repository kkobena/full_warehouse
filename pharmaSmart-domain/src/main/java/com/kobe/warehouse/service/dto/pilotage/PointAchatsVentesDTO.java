package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** Achats et coût des ventes d'une tranche, HT : un écart durable, c'est du stock qui gonfle ou qui fond. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PointAchatsVentesDTO(LocalDate debut, LocalDate fin, String libelle, long achatsHt, long coutVentesHt) {}
