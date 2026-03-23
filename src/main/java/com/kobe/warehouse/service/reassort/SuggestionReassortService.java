package com.kobe.warehouse.service.reassort;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import com.kobe.warehouse.service.reassort.dto.ReassortRecord;
import com.kobe.warehouse.service.reassort.dto.SuggestionReassortDto;

import java.util.List;
import java.util.Set;

public interface SuggestionReassortService {
    void createLigneReassort(Produit p, int totalQty, int newQty);

    void createLigneReassort(List<ReassortRecord> reassortRecords);

    /**
     * Variante utilisable hors SecurityContext (ex : thread @Async).
     * L'appelant fournit explicitement l'utilisateur.
     */
    void createLigneReassort(List<ReassortRecord> reassortRecords, AppUser user);

    void deleteLigneReassort(Integer id);

    void updateLigneReassort(Integer id, int quantity);

    void validateSuggestionReassort(Integer suggestionId);

    void deleteSuggestionReassort(Integer suggestionId);

    List<SuggestionReassortDto> getOpenningSuggestions(TypeReassort typeReassort);

    /**
     * crée une ligne de suggestion de réassort pour le stockProduit donné
     * Pendant la vente en magasin, on peut détecter qu'un produit doit être réassorti.
     * Pendant l'ajustement de stock, on peut détecter qu'un produit doit être réassorti.
     *
     * @param stockProduit
     */
    void createRayonSuggestionReassort(StockProduit stockProduit);

    void createReserveSuggestionReassort(StockProduit stockProduit);

    /**
     * Auto-exécute les suggestions de réassort RESERVE créées durant la réception d'une commande,
     * pour les produits identifiés par {@code produitIds}.
     *
     * <p>Pour chaque {@link com.kobe.warehouse.domain.LigneReassort} de la suggestion OPEN de type
     * {@link com.kobe.warehouse.domain.enumeration.TypeReassort#RESERVE} dont le produit est dans
     * {@code produitIds}, le transfert rayon → réserve est exécuté immédiatement via
     * {@code RepartitionStockService.autoPutawayRayonToReserve()} (avec traçabilité FEFO).
     * La ligne est ensuite retirée de la suggestion ; si la suggestion est vide, elle est clôturée.</p>
     *
     * <p>Doit être appelé APRÈS {@code saveLotStockLocations()} afin que les lots soient déjà
     * présents dans PRINCIPAL avant le transfert FEFO.</p>
     *
     * @param produitIds identifiants des produits reçus dans la commande
     */
    void autoExecuteOverflowForProducts(Set<Integer> produitIds);
}
