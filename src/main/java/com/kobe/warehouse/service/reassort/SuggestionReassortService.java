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

    void deleteLigneReassort(Integer id);

    void updateLigneReassort(Integer id, int quantity);

    void validateSuggestionReassort(Integer suggestionId);

    void deleteSuggestionReassort(Integer suggestionId);

    List<SuggestionReassortDto> getOpenningSuggestions(TypeReassort typeReassort);

    /**
     * crée une ligne de suggestion de réassort pour le stockProduit donné
     * Pendant la vente en magasin, on peut détecter qu'un produit doit être réassorti.
     * Pendant l'ajustement de stock, on peut détecter qu'un produit doit être réassorti.
     * @param stockProduit
     */
    void createSuggestionReassort(StockProduit stockProduit, AppUser user);

}
