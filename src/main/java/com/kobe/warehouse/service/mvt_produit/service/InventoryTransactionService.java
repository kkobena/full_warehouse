package com.kobe.warehouse.service.mvt_produit.service;


import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
