package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ProductState;
import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import com.kobe.warehouse.service.dto.ProductStateEnumProjection;
import jakarta.mail.search.SearchTerm;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/** Spring Data repository for the ProductState entity. */
@SuppressWarnings("unused")
@Repository
public interface ProductStateRepository extends JpaRepository<ProductState, Long> {
    List<ProductState> findProductStateByProduitId(Long produitId);

    List<ProductState> findProductStateByStateAndProduitId(ProductStateEnum productStateEnum, Long produitId);

    boolean existsByStateAndProduitId(ProductStateEnum productStateEnum, Long produitId);

    Set<ProductStateEnumProjection> findDistinctByProduitId(Long produitId);

    void removeProductStateByProduitIdAndState(Long produitId, @NotNull ProductStateEnum state);
}
