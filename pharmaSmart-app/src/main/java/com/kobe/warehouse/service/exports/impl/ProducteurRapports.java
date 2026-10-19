package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.RapportExport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Aiguille chaque édition du catalogue vers le service de son écran : rien n'est recalculé ici, et le bouton de l'écran d'origine
 * continue d'appeler le même service. Les réglages reçus deviennent la requête que ce service attend ; ce qui n'a pas été réglé
 * prend le défaut de la requête, c'est-à-dire celui de l'écran.
 *
 * <p>Un producteur par famille d'écrans : la liste des éditions s'allonge, mais aucune classe ne finit par dépendre de quinze
 * services. L'aiguillage reste exhaustif, le compilateur le vérifie sur l'énumération.
 */
@Component
class ProducteurRapports {

    private final ProducteurRapportsComptables comptables;
    private final ProducteurRapportsPilotage pilotage;
    private final ProducteurRapportsVentes ventes;
    private final ProducteurRapportsStock stock;
    private final ProducteurRapportsPartenaires partenaires;

    ProducteurRapports(
        ProducteurRapportsComptables comptables,
        ProducteurRapportsPilotage pilotage,
        ProducteurRapportsVentes ventes,
        ProducteurRapportsStock stock,
        ProducteurRapportsPartenaires partenaires
    ) {
        this.comptables = comptables;
        this.pilotage = pilotage;
        this.ventes = ventes;
        this.stock = stock;
        this.partenaires = partenaires;
    }

    byte[] produire(RapportExport rapport, FormatExport format, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres) {
        return switch (rapport) {
            case EXPORT_COMPTABLE,
                BALANCE_CAISSE,
                TABLEAU_PHARMACIEN,
                RAPPORT_TVA,
                DECLARATION_TVA,
                RECAPITULATIF_CAISSE,
                RAPPORT_ACTIVITE,
                MOUVEMENTS_CAISSE_ETAT,
                DIFFERES_IMPAYES -> comptables.produire(rapport, format, du, au, filtres);
            case PILOTAGE_TABLEAU_DE_BORD, PILOTAGE_ANALYSER, PILOTAGE_ANNEES -> pilotage.produire(rapport, du, au, filtres);
            case TABLEAU_DE_BORD_CA, MEILLEURES_VENTES, COMPARATIF_VENTES, RENTABILITE_PRODUITS, SEGMENTATION_CLIENTS -> ventes.produire(
                rapport,
                format,
                du,
                au,
                filtres
            );
            case ALERTES_STOCK, ROTATION_STOCK, VALORISATION_STOCK, ABC_PARETO, PRODUITS_A_DETRUIRE -> stock.produire(
                rapport,
                format,
                du,
                au,
                filtres
            );
            case COMPTES_FOURNISSEURS,
                PERFORMANCE_FOURNISSEURS,
                CREANCES_TIERS_PAYANTS,
                RECAPITULATIF_FACTURES,
                RAPPROCHEMENT_TIERS_PAYANTS -> partenaires.produire(rapport, format, du, au, filtres);
        };
    }
}
