package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Fournisseur_;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.SuggestionLine_;
import com.kobe.warehouse.domain.Suggestion_;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import jakarta.persistence.criteria.SetJoin;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the Suggestion entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SuggestionRepository
    extends JpaRepository<Suggestion, Long>, JpaSpecificationExecutor<Suggestion>, SuggestionCustomRepository {
    Optional<Suggestion> findByTypeSuggessionAndFournisseurIdAndMagasinId(
        TypeSuggession typeSuggession,
        Long fournisseurId,
        Long magasinId
    );

    default Specification<Suggestion> filterByType(TypeSuggession typeSuggession) {
        return (root, query, cb) -> cb.equal(root.get(Suggestion_.typeSuggession), typeSuggession);
    }

    default Specification<Suggestion> filterByFournisseurId(Long fournisseurId) {
        return (root, query, cb) -> cb.equal(root.get(Suggestion_.fournisseur).get(Fournisseur_.id), fournisseurId);
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
     * Filter by date
     * @param maxDays nombre de jours conservation des suggestions
     * @return the specification
     */
    default Specification<Suggestion> filterByDate(int maxDays) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(Suggestion_.updatedAt), LocalDateTime.now().minusDays(maxDays));
    }
}
