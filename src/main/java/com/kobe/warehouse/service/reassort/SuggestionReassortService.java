package com.kobe.warehouse.service.reassort;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import com.kobe.warehouse.service.reassort.dto.ReassortRecord;
import com.kobe.warehouse.service.reassort.dto.SuggestionReassortDto;

import java.util.List;

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

   // void createSuggestionReassort(List<StockProduit> stockProduits, AppUser user);

}
