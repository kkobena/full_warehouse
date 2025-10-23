package com.kobe.warehouse.service.mvt_produit.service;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingSum;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InventoryTransactionService {
    void save(Object entity); // Generic save method for all entities

    long quantitySold(Long produitId);

    Optional<InventoryTransaction> findById(Long id);

    Page<InventoryTransactionDTO> getAllInventoryTransactions(
        Pageable pageable,
        Long produitId,
        String startDate,
        String endDate,
        Integer type
    );

    LocalDateTime fetchLastDateByTypeAndProduitId(MouvementProduit type, Long produitId);

    List<ProduitAuditingState> fetchProduitDailyTransaction(@Valid ProduitAuditingParam produitAuditingParam);

    List<ProduitAuditingSum> fetchProduitDailyTransactionSum(@Valid ProduitAuditingParam produitAuditingParam);

    void saveAll(List<OrderLine> entities);
}
