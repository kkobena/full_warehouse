package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingSum;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface InventoryTransactionCustomRepository {
    Page<ProduitAuditingState> fetchProduitDailyTransaction(Specification<InventoryTransaction> specification, Pageable pageable);

    List<ProduitAuditingSum> fetchProduitDailyTransactionSum(Specification<InventoryTransaction> specification);
}
