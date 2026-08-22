package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.domain.Sales;

/**
 * Établit le chiffre d'affaires à déclarer d'une vente, distinct du chiffre encaissé.
 *
 * <p>Cf. docs/PLAN-DECLARATION-CA-EXCLUSIONS-PONCTION.md.
 */
public interface DeclarationCaService {
    /**
     * Applique les exclusions à une vente close et répartit la réduction sur ses règlements.
     *
     * <p>À appeler <strong>après</strong> la création des règlements et avant la persistance : la
     * répartition espèces-d'abord n'a rien sur quoi mordre tant qu'ils n'existent pas.
     *
     * <p>Sans effet si aucun module n'est souscrit, ou si la vente n'est pas concernée par le
     * périmètre (vente dépôt, par exemple).
     *
     * @param sales la vente close, ses lignes et ses règlements chargés
     */
    void appliquerExclusions(Sales sales);
}
