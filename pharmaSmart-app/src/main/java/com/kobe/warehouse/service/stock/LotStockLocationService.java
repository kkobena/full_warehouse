package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Storage;
import java.util.List;

/**
 * Gestion de la table {@code lot_stock_location} — quantité d'un lot par emplacement.
 *
 * <p>Deux responsabilités :</p>
 * <ol>
 *   <li><b>Réception</b> : créditer un lot dans le storage PRINCIPAL lors d'une entrée de stock.</li>
 *   <li><b>Transfert FEFO</b> : déplacer {@code qty} unités d'un storage source vers
 *       un storage destination en respectant l'ordre First-Expired-First-Out.</li>
 * </ol>
 */
public interface LotStockLocationService {

    /**
     * Crédite {@code qtyDelta} unités d'un lot dans un storage (upsert).
     * Crée l'entrée si elle n'existe pas encore.
     *
     * @param lot      lot à créditer
     * @param storage  emplacement cible
     * @param qtyDelta quantité à ajouter (> 0)
     */
    void credit(Lot lot, Storage storage, int qtyDelta);

    /**
     * Débite {@code qtyDelta} unités d'un lot dans un storage.
     * Supprime l'entrée si la quantité atteint zéro.
     *
     * @param lot      lot à débiter
     * @param storage  emplacement source
     * @param qtyDelta quantité à retirer (> 0)
     */
    void debit(Lot lot, Storage storage, int qtyDelta);

    /**
     * Crédite en masse à partir d'une liste de snapshots {@code LotSold} (annulation de vente).
     * Identifie chaque lot par son {@code id} et crédite le storage donné.
     *
     * @param lots    liste des lots vendus à restaurer
     * @param storage emplacement à créditer
     */
    void creditFromSold(List<LotSold> lots, Storage storage);

    /**
     * Crédite {@code qty} unités sur le lot le plus récemment reçu ({@code createdDate DESC})
     * du produit dans ce storage. Met à jour {@code lot.current_quantity} en cohérence.
     * Utilisé pour les ajustements IN sans granularité lot (correction d'oubli de réception).
     * Sans effet si aucun lot n'existe pour ce produit dans ce storage.
     *
     * @param produit produit concerné
     * @param storage emplacement cible
     * @param qty     quantité à ajouter
     */
    void creditLastLot(Produit produit, Storage storage, int qty);

    /**
     * Débite {@code qty} unités d'un produit dans un storage en ordre FEFO,
     * sans destination (ajustement, perte, destruction).
     * S'arrête naturellement si le stock FEFO est épuisé avant {@code qty}.
     *
     * @param produit produit concerné
     * @param storage emplacement source
     * @param qty     quantité à retirer
     */
    void debitFefo(Produit produit, Storage storage, int qty);

    /**
     * Transfère {@code qty} unités d'un produit du storage {@code src} vers {@code dest}
     * en ordre FEFO (lot expirant le plus tôt en premier).
     *
     * <p>Si la quantité disponible est inférieure à {@code qty}, transfère le maximum possible.</p>
     *
     * @param produit produit concerné
     * @param src     storage source (ex : PRINCIPAL / rayon)
     * @param dest    storage destination (ex : SAFETY_STOCK / réserve)
     * @param qty     quantité à transférer
     */
    void transferFefo(Produit produit, Storage src, Storage dest, int qty);
}
