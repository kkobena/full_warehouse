package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit_;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.SuggestionLine_;
import com.kobe.warehouse.domain.Suggestion_;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.projection.SuggestionAggregator;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data repository for the SuggestionLine entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SuggestionLineRepository extends JpaRepository<SuggestionLine, Integer>, JpaSpecificationExecutor<SuggestionLine> {
    Optional<SuggestionLine> findBySuggestionTypeSuggessionAndFournisseurProduitId(
        TypeSuggession typeSuggession,
        Integer fournisseurProduitId
    );

    boolean existsByFournisseurProduitProduitId(Integer produitId);

    int countByFournisseurProduitProduitId(Integer produitId);

    default Specification<SuggestionLine> filterBySuggestionId(Integer suggestionId) {
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
                cb.like(cb.upper(root.get(SuggestionLine_.fournisseurProduit).get(FournisseurProduit_.codeEan)), searchPattern),
                cb.like(
                    cb.upper(
                        root.get(SuggestionLine_.fournisseurProduit).get(FournisseurProduit_.produit).get(Produit_.codeEanLaboratoire)
                    ),
                    searchPattern
                )
            );
        };
    }

    Optional<SuggestionLine> findBySuggestionIdAndFournisseurProduitProduitId(Integer suggestionId, Integer produitId);

    @Query(
        nativeQuery = true,
        value = "SELECT COUNT(sug_line.id) AS itemsCount,SUM(sug_line.quantity*fp.prix_achat) AS montantAchat ,SUM(sug_line.quantity*fp.prix_uni) AS montantVente FROM suggestion_line sug_line JOIN fournisseur_produit fp ON fp.id=sug_line.fournisseur_produit_id WHERE sug_line.suggestion_id = ?1"
    )
    SuggestionAggregator getSuggestionData(Integer suggestionId);
}
