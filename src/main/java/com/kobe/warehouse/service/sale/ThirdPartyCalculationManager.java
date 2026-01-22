package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySales;

import java.util.List;

/**
 * Service dédié aux calculs des montants et parts dans les ventes avec tiers-payants.
 * Ce service encapsule toute la logique complexe de répartition des montants entre
 * l'assuré et les différents tiers-payants en fonction de leurs taux de prise en charge.
 */
public interface ThirdPartyCalculationManager {

    /**
     * Met à jour les montants d'une vente avec tiers-payants.
     * Calcule les parts assuré et tiers-payant en fonction des taux de prise en charge.
     *
     * @param thirdPartySales la vente à mettre à jour
     * @param isUpdate true si c'est une mise à jour (vérifie les plafonds), false sinon
     * @param clientTiersPayants la liste des clients tiers-payants
     * @return message d'erreur si plafond dépassé, null sinon
     */
    String upddateThirdPartySaleAmounts(
        ThirdPartySales thirdPartySales,
        boolean isUpdate,
        List<ClientTiersPayant> clientTiersPayants
    );

    /**
     * Recalcule et applique les montants sur une vente avec tiers-payants.
     * Cette méthode réalise le calcul complet avec appel au service de calcul externe.
     *
     * @param thirdPartySales la vente à recalculer
     * @param clientTiersPayants la liste des clients tiers-payants (peut être null)
     * @param isUpdate true si c'est une mise à jour
     * @return message d'erreur si plafond dépassé, null sinon
     */
    String reComputeAndApplyAmounts(
        ThirdPartySales thirdPartySales,
        List<ClientTiersPayant> clientTiersPayants,
        boolean isUpdate
    );

    /**
     * Calcule les montants d'une vente avec tiers-payants.
     * Version simplifiée sans paramètre isUpdate.
     *
     * @param thirdPartySales la vente
     * @return message d'erreur si plafond dépassé, null sinon
     */
    String computeThirdPartySaleAmounts(ThirdPartySales thirdPartySales);

    /**
     * Met à jour les montants lors de la suppression d'un article.
     * Recalcule les parts après suppression d'une ligne de vente.
     *
     * @param thirdPartySales la vente
     */
    void upddateSaleAmountsOnRemovingItem(ThirdPartySales thirdPartySales);
}
