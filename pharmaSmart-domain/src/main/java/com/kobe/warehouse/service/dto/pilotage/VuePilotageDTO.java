package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.AffichageAnalyse;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Réglage enregistré de l'onglet Analyser (la période vient de la barre, pas de la vue).
 *
 * @param livree livrée d'office : ni modifiable, ni supprimable
 * @param modifiable l'utilisateur courant en est le propriétaire
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record VuePilotageDTO(
    Long id,
    @NotBlank @Size(max = 100) String libelle,
    @NotEmpty @Size(max = 3) List<IndicateurPilotage> indicateurs,
    @NotNull AxeAnalyse axe,
    AxeAnalyse axe2,
    int top,
    @NotNull TriAnalyse tri,
    @NotNull AffichageAnalyse affichage,
    boolean livree,
    boolean partagee,
    boolean modifiable
) {}
