package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.MvStockValuationByRayonView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface MvStockValuationRayonViewRepository extends JpaRepository<MvStockValuationByRayonView, Integer>, JpaSpecificationExecutor<MvStockValuationByRayonView> {

    List<MvStockValuationByRayonView> findAllByOrderByTotalSalesValueDesc();

    Page<MvStockValuationByRayonView> findAllByOrderByTotalSalesValueDesc(Pageable pageable);

    default Specification<MvStockValuationByRayonView> filterByFamilleProduitId(Integer familleProduitId) {
        if (familleProduitId == null) {
            return null;
        }
        return null;
      //  return (root, query, cb) -> cb.equal(root.get(MvStockValuationByRayonView_.categorieId), familleProduitId);
    }

    default Specification<MvStockValuationByRayonView> filterByRayonId(Integer rayonId) {
        return null;
        //  return (root, query, cb) -> cb.equal(root.get(MvStockValuationByRayonView_.rayonId), rayonId);
    }
}
