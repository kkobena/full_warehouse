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
     * Liste paginée des suggestions, avec filtres optionnels.
     * @param statut filtre par statut (GENEREE = Réapprovisionnement, VALIDEE = Commandes à passer). null = tous.
     */
    Page<SuggestionProjection> getAllSuggestion(String search, Set<Integer> fournisseurIds,
        TypeSuggession typeSuggession, Set<StatutSuggession> statut, Pageable pageable);

    /**
     * Compte les suggestions par statut — utilisé pour les badges onglets de l'UI.
     */
    long countByStatut(StatutSuggession statut);

    /**
     * Résumé agrégé par fournisseur, avec filtres optionnels.
     * @param statut         filtre par statut (null ou vide = tous)
     * @param fournisseurIds filtre sur une liste de fournisseurs (null ou vide = tous)
     * @param searchTerm     filtre produit sur codeCip, codeEan, libelle, codeEanLaboratoire (null = aucun filtre)
     */
    List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur(Set<StatutSuggession> statut, Set<Integer> fournisseurIds, String searchTerm);

    default List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur() {
        return getSuggestionsParFournisseur(null, null, null);
    }

    default List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur(Set<StatutSuggession> statut) {
        return getSuggestionsParFournisseur(statut, null, null);
    }

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
