package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface SuggestionCustomRepository {
    Page<SuggestionProjection> getAllSuggestion(Specification<Suggestion> specification, Pageable pageable);

    /** Retourne un résumé agrégé par fournisseur pour les suggestions récentes (≤ retentionDays). */
    List<FournisseurSuggestionSummaryDTO> getParFournisseur(int retentionDays);

    /** Retourne un résumé agrégé par fournisseur, filtré par statut (GENEREE, VALIDEE…). */
    List<FournisseurSuggestionSummaryDTO> getParFournisseur(int retentionDays, StatutSuggession statut);
}
