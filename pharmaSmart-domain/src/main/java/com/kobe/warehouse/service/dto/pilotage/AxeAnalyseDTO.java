package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import java.util.Set;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record AxeAnalyseDTO(AxeAnalyse code, String libelle, Set<SourceAnalyse> sources, AxeAnalyse suivant, boolean filtrable, boolean ordonne) {
    public static AxeAnalyseDTO fromAxe(AxeAnalyse axe) {
        return new AxeAnalyseDTO(axe, axe.getLibelle(), axe.getSources(), axe.suivant(), axe.estLuEnBase(), axe.estOrdonne());
    }
}
