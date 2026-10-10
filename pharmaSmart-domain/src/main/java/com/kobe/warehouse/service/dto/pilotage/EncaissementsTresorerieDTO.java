package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Sous-section Encaissements & caisse : règlements de ventes, de différés et de factures tiers payant.
 *
 * @param tranches libellés des tranches de la période, dans l'ordre des séries
 * @param caissiers {@code null} sans le droit « Clients & équipe »
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record EncaissementsTresorerieDTO(
    ComparaisonPeriodesDTO comparaison,
    CelluleAnalyseDTO total,
    List<ModeEncaissementDTO> modes,
    List<String> tranches,
    List<SerieModeDTO> series,
    List<CaissierEncaissementDTO> caissiers
) {}
