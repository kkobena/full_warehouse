package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ProductState;
import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/** Spring Data repository for the ProductState entity. */
@SuppressWarnings("unused")
@Repository
public interface ProductStateRepository extends JpaRepository<ProductState, Long> {
  List<ProductState> findProductStateByProduitId(Long produitId);

  List<ProductState> findProductStateByStateAndProduitId(
      ProductStateEnum productStateEnum, Long produitId);
}
