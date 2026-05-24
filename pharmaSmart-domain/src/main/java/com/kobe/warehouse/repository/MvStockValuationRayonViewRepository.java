package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.MvStockValuationByRayonView;
import com.kobe.warehouse.domain.MvStockValuationByRayonView_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


@Repository
public interface MvStockValuationRayonViewRepository extends JpaRepository<MvStockValuationByRayonView, Integer>, JpaSpecificationExecutor<MvStockValuationByRayonView> {


    default Specification<MvStockValuationByRayonView> filterByMagasinId(Integer magasinId) {
        if (magasinId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("magasinId"), magasinId);
    }

    default Specification<MvStockValuationByRayonView> filterByFamilleProduitId(Integer familleProduitId) {
        if (familleProduitId == null) return null;
        return (root, query, cb) -> cb.equal(root.get(MvStockValuationByRayonView_.categorieId), familleProduitId);
    }

    default Specification<MvStockValuationByRayonView> filterByRayonId(Integer rayonId) {
        return (root, query, cb) -> cb.equal(root.get(MvStockValuationByRayonView_.rayonId), rayonId);
    }
}
