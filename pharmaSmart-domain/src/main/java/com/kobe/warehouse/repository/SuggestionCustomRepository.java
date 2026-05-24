package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface SuggestionCustomRepository {
    Page<SuggestionProjection> getAllSuggestion(Specification<Suggestion> specification, Pageable pageable, String searchTerm);

    /**
     * Résumé agrégé par fournisseur, avec filtres optionnels.
     * @param statut         filtre par statut (null ou vide = tous)
     * @param fournisseurIds filtre sur une liste de fournisseurs (null ou vide = tous)
     * @param searchTerm     filtre produit sur codeCip, codeEan, libelle, codeEanLaboratoire (null = aucun filtre)
     */
    List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur(Set<StatutSuggession> statut, Set<Integer> fournisseurIds, String searchTerm);


}
