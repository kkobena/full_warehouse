package com.kobe.warehouse.service.dto.projection;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import java.math.BigDecimal;

public interface MouvementCaisse {
    /**
     * Le libellé court : ces mouvements s'affichent en cartes et en colonnes étroites
     * (rapport d'activité, récapitulatif de caisse), où « Règlement facture fournisseur »
     * déborde sur le montant.
     */
    default String getLibelle() {
        return getType().getTransactionTypeAffichage().getValueCourt();
    }

    BigDecimal getMontant();

    TypeFinancialTransaction getType();
}
