package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the SuggestionLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SuggestionLineRepository extends JpaRepository<SuggestionLine, Long>, JpaSpecificationExecutor<SuggestionLine> {
    Optional<SuggestionLine> findBySuggestionTypeSuggessionAndFournisseurProduitId(TypeSuggession typeSuggession, Long produitId);

    boolean existsByFournisseurProduitProduitId(Long produitId);

    default Specification<SuggestionLine> filterBySuggestionId(long suggestionId) {
        return (root, query, cb) -> cb.equal(root.get(SuggestionLine_.suggestion).get(Suggestion_.id), suggestionId);
    }

    default Specification<SuggestionLine> filterByProduit(String search) {
        return (root, query, cb) -> {
            String searchPattern = search.toUpperCase() + "%";
            return cb.or(
                cb.like(cb.upper(root.get(SuggestionLine_.fournisseurProduit).get(FournisseurProduit_.codeCip)), searchPattern),
                cb.like(
                    cb.upper(root.get(SuggestionLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.libelle)),
                    searchPattern
                ),
                cb.like(
                    cb.upper(root.get(SuggestionLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.codeEan)),
                    searchPattern
                )
            );
        };
    }
}
