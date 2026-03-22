package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.BudgetCommandeDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.dto.records.QuantitySuggestion;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.stock.dto.QauntiteProduitVendus;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SuggestionProduitService {
    void suggerer(List<QuantitySuggestion> quantitySuggestions, Magasin magasin, AppUser user);

    Page<SuggestionProjection> getAllSuggestion(String search, Integer fournisseurId, TypeSuggession typeSuggession, Pageable pageable);

    List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur();

    Optional<SuggestionDTO> getSuggestionById(Integer id);


    Page<SuggestionLineDTO> getSuggestionLinesByIdWithConsommation(Integer suggestionId, String search, String niveauUrgence, Pageable pageable);

    void fusionnerSuggestion(Set<Integer> ids) throws GenericError;

    void deleteSuggestion(Set<Integer> ids);

    void deleteSuggestionLine(Set<Integer> ids);

    void sanitize(Integer suggestionId);

    void commander(Integer suggestionId);

    void commanderSelection(CommanderSelectionDTO dto);

    BudgetCommandeDTO getBudgetCommande();

    void validerSuggestion(Integer id);

    void rejeterSuggestion(Integer id);

    void addSuggestionLine(Integer suggestionId, SuggestionLineDTO suggestionLine);

    void updateSuggestionLinQuantity(SuggestionLineDTO suggestionLine);

    byte[] exportToCsv(Integer id) throws IOException;

    byte[] exportToPdf(Integer id);

    int suggestionQuantiteProduitVendus(List<QauntiteProduitVendus> produitVendus, Boolean suggerQuantitySold);
}
