package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.criteria.InventoryTransactionSpec;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import com.kobe.warehouse.service.dto.filter.InventoryTransactionFilterDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryTransactionService {

    private final InventoryTransactionRepository inventoryTransactionRepository;

    private final InventoryTransactionSpec inventoryTransactionSpec;

    public InventoryTransactionService(
        InventoryTransactionRepository inventoryTransactionRepository,
        InventoryTransactionSpec inventoryTransactionSpec
    ) {
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryTransactionSpec = inventoryTransactionSpec;
    }

    @Transactional(readOnly = true)
    public long quantitySold(Long produitId) {
        Long aLong = inventoryTransactionRepository.quantitySold(TransactionType.SALE, produitId);
        return (aLong != null ? aLong : 0);
    }

    @Transactional(readOnly = true)
    public Optional<InventoryTransaction> findById(Long id) {
        return inventoryTransactionRepository.findById(id);
    }

    private Specification<InventoryTransaction> add(
        Specification<InventoryTransaction> specification,
        Specification<InventoryTransaction> current
    ) {
        if (current == null) {
            current = Specification.where(specification);
        } else {
            current = specification.and(specification);
        }
        return current;
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransactionDTO> getAllInventoryTransactions(
        Pageable pageable,
        Long produitId,
        String startDate,
        String endDate,
        Integer type
    ) {
        this.inventoryTransactionSpec.setInventoryTransactionFilter(
                new InventoryTransactionFilterDTO().setEndDate(endDate).setProduitId(produitId).setStartDate(startDate).setType(type)
            );

        return inventoryTransactionRepository.findAll(this.inventoryTransactionSpec, pageable).map(InventoryTransactionDTO::new);
    }
}
