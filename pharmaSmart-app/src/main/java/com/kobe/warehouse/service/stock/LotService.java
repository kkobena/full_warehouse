package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.stock.dto.LotFilterParam;
import com.kobe.warehouse.service.stock.dto.LotPerimeDTO;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface LotService {
    /**
     * Methode à utiliser uniquement dans le process de commande normale pas dans BED
     * @param lot
     * @return
     */
    LotDTO addLot(LotDTO lot);

    List<LotDTO> addLotBatch(List<LotDTO> lots);

    /**
     * Crée un lot directement rattaché à un produit, sans OrderLine.
     * Utilisé pour la saisie de lot hors commande depuis la fiche produit.
     * <ul>
     *   <li>numLot et expiryDate sont obligatoires.</li>
     *   <li>La quantité doit être ≤ au stock total du produit.</li>
     *   <li>Aucun mouvement de stock n'est généré.</li>
     * </ul>
     *
     * @param lot DTO contenant produitId, numLot, expiryDate, quantity
     * @return le lot créé
     */
    LotDTO addLotSurProduit(LotDTO lot);

    LotDTO editLot(LotDTO lot);

    void remove(LotDTO lot);

    void remove(Integer lotId);

    List<Lot> findByProduitId(Integer produitId);

    List<Lot> findProduitLots(Integer produitId);

    void updateLots(List<LotSold> lots);

    void restoreLots(List<LotSold> lots);

    /**
     * Applique un delta sur {@code lot.current_quantity} suite à un ajustement manuel.
     * <ul>
     *   <li>{@code qtyDelta < 0} (OUT) : débite les lots en ordre FEFO jusqu'à épuisement du delta.</li>
     *   <li>{@code qtyDelta > 0} (IN)  : crédite le lot le plus récemment reçu ({@code createdDate DESC}).</li>
     * </ul>
     * Sans effet si le produit n'a aucun lot.
     *
     * @param produit  produit ajusté
     * @param qtyDelta delta signé (négatif = sortie, positif = entrée)
     */
    void adjustLots(Produit produit, int qtyDelta);

    /**
     * Crédite un lot explicitement sélectionné (AJUSTEMENT_IN avec gestion_lot=true).
     * Met à jour {@code lot.current_quantity} et réactive le lot si nécessaire.
     *
     * @param lot lot à créditer
     * @param qty quantité à ajouter (positive)
     */
    void creditSpecificLot(Lot lot, int qty);

    Page<LotPerimeDTO> findLotsPerimes(LotFilterParam lotFilterParam, Pageable pageable);

    LotPerimeValeurSum findPerimeSum(LotFilterParam lotFilterParam);

    ResponseEntity<byte[]> generatePdf(LotFilterParam lotFilterParam);
}
