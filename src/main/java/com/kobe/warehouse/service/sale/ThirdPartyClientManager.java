package com.kobe.warehouse.service.sale;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.NumBonAlreadyUseException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service dédié à la gestion des tiers-payants et de leurs lignes de vente.
 * Ce service encapsule toute la logique liée aux clients tiers-payants,
 * à la vérification des numéros de bon, et à la gestion des consommations.
 */
public interface ThirdPartyClientManager {

    /**
     * Récupère les clients tiers-payants à partir de leurs identifiants.
     *
     * @param ids ensemble des identifiants des clients tiers-payants
     * @return liste des clients tiers-payants trouvés
     */
    List<ClientTiersPayant> getClientTiersPayants(Set<Integer> ids);

    /**
     * Sauvegarde les lignes de tiers-payants pour une vente.
     * Crée les lignes de ThirdPartySaleLine et les associe à la vente.
     *
     * @param dto             la vente contenant les tiers-payants
     * @param thirdPartySales la vente à laquelle associer les lignes
     * @return message d'erreur si plafond dépassé, null sinon
     * @throws NumBonAlreadyUseException si le numéro de bon est déjà utilisé
     * @throws GenericError              si un tiers-payant n'est pas trouvé
     */
    String saveTiersPayantLines(ThirdPartySaleDTO dto, ThirdPartySales thirdPartySales)
        throws NumBonAlreadyUseException, GenericError;

    /**
     * Vérifie si un numéro de bon est déjà utilisé pour un client tiers-payant.
     *
     * @param numBon              le numéro de bon à vérifier
     * @param clientTiersPayantId l'identifiant du client tiers-payant
     * @param currentSaleId       l'identifiant de la vente courante (null pour une nouvelle vente)
     * @return true si le numéro de bon est déjà utilisé, false sinon
     */
    boolean checkIfNumBonIsAlReadyUse(String numBon, Integer clientTiersPayantId, Long currentSaleId);

    /**
     * Ajoute une ligne de tiers-payant à une vente existante.
     *
     * @param dto    les informations du client tiers-payant à ajouter
     * @param saleId l'identifiant de la vente
     * @return message d'erreur si plafond dépassé, null sinon
     * @throws NumBonAlreadyUseException si le numéro de bon est déjà utilisé
     * @throws GenericError              en cas d'erreur métier
     */
    String addThirdPartySaleLineToSales(ClientTiersPayantDTO dto, SaleId saleId)
        throws NumBonAlreadyUseException, GenericError;

    /**
     * Supprime une ligne de tiers-payant d'une vente.
     *
     * @param clientTiersPayantId l'identifiant du client tiers-payant à supprimer
     * @param saleId              l'identifiant de la vente
     * @return message d'erreur si plafond dépassé après suppression, null sinon
     */
    String removeThirdPartySaleLineToSales(Integer clientTiersPayantId, SaleId saleId);

    /**
     * Met à jour le compte du client tiers-payant suite à une vente.
     *
     * @param thirdPartySaleLine la ligne de vente tiers-payant
     */
    void updateClientTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine);

    /**
     * Met à jour le compte du tiers-payant (organisme) suite à une vente.
     *
     * @param thirdPartySaleLine la ligne de vente tiers-payant
     */
    void updateTiersPayantAccount(ThirdPartySaleLine thirdPartySaleLine);


    /**
     * Récupère toutes les lignes de tiers-payant d'une vente.
     *
     * @param saleId l'identifiant de la vente
     * @return la liste des lignes de tiers-payant
     */
    List<ThirdPartySaleLine> findAllBySaleId(SaleId saleId);

    /**
     * Clone une ligne de tiers-payant pour une copie de vente (annulation).
     *
     * @param original la ligne originale
     * @param copy     la copie de la vente
     * @return la ligne clonée
     */
    ThirdPartySaleLine clone(ThirdPartySaleLine original, ThirdPartySales copy);

    List<ThirdPartySaleLine> clone(List<ThirdPartySaleLine> originals, ThirdPartySales copy);

    void saveAll(List<ThirdPartySaleLine> thirdPartySaleLines);

    /**
     * Met à jour une ligne de tiers-payant existante.
     *
     * @param numBon              le nouveau numéro de bon
     * @param thirdPartySaleLine  la ligne à mettre à jour
     * @param clientTiersPayantId l'identifiant du client tiers-payant
     * @param montant             le montant
     * @throws NumBonAlreadyUseException si le numéro de bon est déjà utilisé
     */
    void updateThirdPartySaleLine(
        String numBon,
        ThirdPartySaleLine thirdPartySaleLine,
        Integer clientTiersPayantId,
        Integer montant
    ) throws NumBonAlreadyUseException;

    /**
     * Trouve une ligne de tiers-payant dans une vente par l'identifiant du client.
     *
     * @param sale                la vente
     * @param clientTiersPayantId l'identifiant du client tiers-payant
     * @return Optional contenant la ligne si trouvée
     */
    Optional<ThirdPartySaleLine> findSaleLineByClientTiersPayantId(ThirdPartySales sale, Integer clientTiersPayantId);

    /**
     * Sauvegarde les lignes de tiers-payants lors d'un changement de client.
     *
     * @param thirdPartySales la vente
     * @return message d'erreur si plafond dépassé, null sinon
     */
    String saveTiersPayantLinesOnChangeCustomer(ThirdPartySales thirdPartySales);
}
