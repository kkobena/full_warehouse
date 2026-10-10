package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Sous-section Tiers payant. Encours, DSO, vieillissement et échéancier sont à date (aujourd'hui) ; facturé et réglé portent sur
 * les factures émises dans la période. Mêmes définitions que le vieillissement des créances : reste dû = net − réglé des factures
 * non soldées, hors factures rattachées à une facture de groupe.
 *
 * @param encaissementsAttendus échéancier : en retard, puis mois par mois
 * @param seuilHistorique nombre de factures réglées à partir duquel le délai observé d'un organisme est retenu
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record TiersPayantTresorerieDTO(
    ComparaisonPeriodesDTO comparaison,
    CelluleAnalyseDTO facture,
    CelluleAnalyseDTO regle,
    long encours,
    Integer dso,
    List<OrganismeTresorerieDTO> organismes,
    List<TrancheMontantDTO> vieillissement,
    List<TrancheMontantDTO> encaissementsAttendus,
    Double concentrationTrois,
    Double concentrationCinq,
    int seuilHistorique
) {}
