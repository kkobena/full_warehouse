package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.SuggestionLine_;
import com.kobe.warehouse.domain.Suggestion_;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import jakarta.persistence.criteria.SetJoin;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the Suggestion entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SuggestionRepository
    extends JpaRepository<Suggestion, Integer>, JpaSpecificationExecutor<Suggestion>, SuggestionCustomRepository {
    Optional<Suggestion> findByTypeSuggessionAndFournisseurIdAndMagasinId(
        TypeSuggession typeSuggession,
        Integer fournisseurId,
        Integer magasinId
    );

    default Specification<Suggestion> filterByType(TypeSuggession typeSuggession) {
        return (root, query, cb) -> cb.equal(root.get(Suggestion_.typeSuggession), typeSuggession);
    }

    default Specification<Suggestion> filterByStatut(EnumSet<StatutSuggession> statut) {
        return (root, query, cb) -> root.get(Suggestion_.statut).in(statut);
    }

    default Specification<Suggestion> filterByFournisseurId(Integer fournisseurId) {
        return (root, query, cb) -> cb.equal(root.get(Suggestion_.fournisseur).get(Fournisseur_.id), fournisseurId);
    }

    default Specification<Suggestion> filterByFournisseurIds(Set<Integer> fournisseurIds) {
        return (root, query, cb) -> root.get(Suggestion_.fournisseur).get(Fournisseur_.id).in(fournisseurIds);
    }

    default Specification<Suggestion> filterByProduit(String search) {
        return (root, query, cb) -> {
            String searchPattern = search.toUpperCase() + "%";
            SetJoin<Suggestion, SuggestionLine> setJoin = root.joinSet(Suggestion_.SUGGESTION_LINES);
            return cb.or(
                cb.like(setJoin.get(SuggestionLine_.fournisseurProduit).get(FournisseurProduit_.codeCip), searchPattern),
                cb.like(setJoin.get(SuggestionLine_.fournisseurProduit).get(FournisseurProduit_.codeEan), searchPattern),
                cb.like(
                    cb.upper(setJoin.get(SuggestionLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.libelle)),
                    searchPattern
                ),
                cb.like(
                    setJoin.get(SuggestionLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.codeEanLaboratoire),
                    searchPattern
                )
            );
        };
    }

    /**
     * Filter by date (rétention)
     * @param maxDays nombre de jours conservation des suggestions
     */
    default Specification<Suggestion> filterByDate(int maxDays) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(Suggestion_.updatedAt), LocalDateTime.now().minusDays(maxDays));
    }

    /**
     * Compte les suggestions par statut (GENEREE, VALIDEE…) pour les badges de l'UI.
     */
    long countByStatut(StatutSuggession statut);

    /**
     * Supprime les suggestions AUTO devenues vides (aucune ligne) et encore au statut GENEREE
     * (jamais traitées par le pharmacien). Appelé en fin de batch SEMOIS après suppression des
     * lignes obsolètes. {@code flushAutomatically} garantit que les suppressions de lignes en
     * attente sont écrites en base avant l'évaluation du prédicat {@code IS EMPTY}.
     *
     * @return nombre de suggestions supprimées
     */
    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM Suggestion s
        WHERE s.typeSuggession = com.kobe.warehouse.domain.enumeration.TypeSuggession.AUTO
          AND s.statut = com.kobe.warehouse.domain.enumeration.StatutSuggession.GENEREE
          AND s.suggestionLines IS EMPTY
        """)
    int deleteEmptyAutoSuggestions();
}
