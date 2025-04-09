package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.projection.SuggestionProjection;
import com.kobe.warehouse.service.dto.records.QuantitySuggestion;
import com.kobe.warehouse.service.errors.GenericError;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SuggestionProduitService {
    void suggerer(List<QuantitySuggestion> quantitySuggestions);

    int suggererListProduits(List<Produit> produits);

    void suggerer(Produit produit);

    Page<SuggestionProjection> getAllSuggestion(String search, Long fournisseurId, TypeSuggession typeSuggession, Pageable pageable);

    Optional<SuggestionDTO> getSuggestionById(long id);

    Page<SuggestionLineDTO> getSuggestionLinesById(long suggestionId, String search, Pageable pageable);

    void fusionnerSuggestion(Set<Long> ids) throws GenericError;

    void deleteSuggestion(Set<Long> ids);

    void deleteSuggestionLine(Set<Long> ids);

    void sanitize(long suggestionId);
}
