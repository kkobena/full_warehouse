package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.MvStockValuationView;
import com.kobe.warehouse.domain.MvStockValuationView_;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour la vue matérialisée mv_semois_suggestion.
 * LECTURE SEULE: Cette vue est rafraîchie automatiquement tous les jours à 3h du matin
 * après le recalcul SEMOIS. Ne jamais tenter d'écrire dans cette vue.
 */
@Repository
public interface MvStockValuationViewRepository extends JpaRepository<MvStockValuationView, Integer> {
    //total_sales_value DESC
    List<MvStockValuationView> findAllByOrderByTotalSalesValueDesc();

    Page<MvStockValuationView> findAllByOrderByTotalSalesValueDesc(Pageable pageable);

    default Specification<MvStockValuationView> filterByFamilleProduitId(Integer familleProduitId) {
        if (familleProduitId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(MvStockValuationView_.categorieId), familleProduitId);
    }
}
