package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** Un vendeur (ou l'équipe entière) : chaque mesure comparée à la référence. Taux et parts en %. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record LigneVendeurDTO(
    String cle,
    String libelle,
    CelluleAnalyseDTO nbVentes,
    CelluleAnalyseDTO caTtc,
    CelluleAnalyseDTO panierMoyen,
    CelluleAnalyseDTO articlesParVente,
    CelluleAnalyseDTO tauxMarge,
    CelluleAnalyseDTO tauxRemise,
    CelluleAnalyseDTO annulations,
    CelluleAnalyseDTO avoirs,
    CelluleAnalyseDTO partOrdonnance
) {}
