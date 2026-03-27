package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.BudgetCommandeDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.SemoisCommanderDTO;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
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

    /**
     * @deprecated Décommissionné depuis v12. Le batch {@code SemoisBatchJobService.creerSuggestionBatch()}
     *             remplace cette approche post-vente.
     */
    @Deprecated(since = "2026-03", forRemoval = false)
    void suggerer(List<QuantitySuggestion> quantitySuggestions, Magasin magasin, AppUser user);

    /**
     * Liste paginée des suggestions, avec filtres optionnels.
     * @param statut filtre par statut (GENEREE = Réapprovisionnement, VALIDEE = Commandes à passer). null = tous.
     */
    Page<SuggestionProjection> getAllSuggestion(String search, Integer fournisseurId,
        TypeSuggession typeSuggession, StatutSuggession statut, Pageable pageable);

    /**
     * Compte les suggestions par statut — utilisé pour les badges onglets de l'UI.
     */
    long countByStatut(StatutSuggession statut);

    List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur();

    /**
     * Liste par fournisseur filtrée par statut (v12).
     * @param statut GENEREE = tab Réapprovisionnement, VALIDEE = tab Commandes à passer
     */
    List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur(StatutSuggession statut);

    Optional<SuggestionDTO> getSuggestionById(Integer id);

    Page<SuggestionLineDTO> getSuggestionLinesByIdWithConsommation(Integer suggestionId, String search, String niveauUrgence, Pageable pageable);

    /** Charge toutes les lignes d'une suggestion sans pagination (usage édition). */
    List<SuggestionLineDTO> getAllSuggestionLines(Integer suggestionId, String search, String niveauUrgence);

    void fusionnerSuggestion(Set<Integer> ids) throws GenericError;

    void deleteSuggestion(Set<Integer> ids);

    void deleteSuggestionLine(Set<Integer> ids);

    void sanitize(Integer suggestionId);

    CommandeId commander(Integer suggestionId, Integer fournisseurId);

    CommandeId commanderSelection(CommanderSelectionDTO dto);

    /**
     * @deprecated Décommissionné depuis  Un seul chemin : {@code Suggestion} → {@code Commande}.
     */
    @Deprecated(since = "2026-03", forRemoval = false)
    void createCommandesFromSemois(List<SemoisCommanderDTO.LigneSemois> lignes);

    BudgetCommandeDTO getBudgetCommande();

    void validerSuggestion(Integer id);

    void rejeterSuggestion(Integer id);

    void addSuggestionLine(Integer suggestionId, SuggestionLineDTO suggestionLine);

    void updateSuggestionLinQuantity(SuggestionLineDTO suggestionLine);

    byte[] exportToCsv(Integer id) throws IOException;

    byte[] exportToPdf(Integer id);

    int suggestionQuantiteProduitVendus(List<QauntiteProduitVendus> produitVendus, Boolean suggerQuantitySold);

    /**
     * Réinitialise le flag {@code quantiteModifieeManuel} d'une ligne
     * (le batch pourra à nouveau mettre à jour sa quantité).
     */
    void resetQuantiteManuelle(Integer suggestionLineId);
}
