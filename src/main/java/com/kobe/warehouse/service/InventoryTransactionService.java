package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryTransactionService {
    private final Logger LOG = LoggerFactory.getLogger(InventoryTransactionService.class);
    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired
    private ProduitRepository produitRepository;
    private final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public long quantitySold(Long produitId) {
        Long aLong = inventoryTransactionRepository.quantitySold(TransactionType.SALE, produitId);
        return (aLong != null ? aLong : 0);
    }

    @Transactional(readOnly = true)
    public long quantitySoldIncludeChildQuantity(Long produitId) {
        Produit produit = produitRepository.getOne(produitId);
        long parentQty = inventoryTransactionRepository.quantitySold(TransactionType.SALE, produitId);
        if (!produit.getProduits().isEmpty()) {
            long childQty = inventoryTransactionRepository.quantitySold(TransactionType.SALE, produit.getProduits().get(0).getId());
            parentQty += ((long) Math.ceil(Double.valueOf(childQty) / produit.getItemQty()));
        }
        return parentQty;
    }

    public void buildInventoryTransaction(StoreInventoryLine storeInventoryLine, DateDimension dateDimension, Instant now, User user) {
        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setCreatedAt(now);
        inventoryTransaction.setProduit(storeInventoryLine.getProduit());
        inventoryTransaction.setUser(user);
        inventoryTransaction.setAmount(storeInventoryLine.getInventoryValueCost());
        inventoryTransaction.setQuantity(storeInventoryLine.getUpdated() ? storeInventoryLine.getQuantityOnHand() : storeInventoryLine.getQuantityInit());
        inventoryTransaction.setTransactionType(TransactionType.INVENTAIRE);
        inventoryTransaction.setQuantityAfter(inventoryTransaction.getQuantity());
        inventoryTransaction.setQuantityBefor(storeInventoryLine.getQuantityInit());
        inventoryTransaction.setDateDimension(dateDimension);
        inventoryTransaction.setCostAmount(storeInventoryLine.getProduit().getCostAmount());
        inventoryTransaction.setRegularUnitPrice(storeInventoryLine.getInventoryValueLatestSellingPrice());
        inventoryTransactionRepository.save(inventoryTransaction);
    }

    @Transactional(readOnly = true)
    public Optional<InventoryTransaction> findById(Long id) {
        return inventoryTransactionRepository.findById(id);
    }

    private Specification<InventoryTransaction> add(Specification<InventoryTransaction> specification, Specification<InventoryTransaction> current) {
        if (current == null) {
            current = Specification.where(specification);
        } else {
            current = specification.and(specification);
        }
        return current;
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDTO> getAllInventoryTransactions(Long produitId, String startDate, String endDate, Integer type) {
        Instant dtStart = null;
        Instant dtEnd = null;
        Specification<InventoryTransaction> specification = null;
        if (type != null) {
            TransactionType transactionType = TransactionType.values()[type];
            specification = Specification.where(inventoryTransactionRepository.specialisationTypeTransaction(transactionType));
        }
        if (produitId != null) {
            add(inventoryTransactionRepository.specialisationProduitId(produitId), specification);
        }
        if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate)) {
            if (!StringUtils.isEmpty(startDate)) {
                LocalDateTime dateStart = LocalDateTime.of(LocalDate.parse(startDate, dateTimeFormatter), LocalTime.of(0, 0, 0));
                dtStart = dateStart.toInstant(ZoneOffset.UTC);
                add(inventoryTransactionRepository.specialisationDateGreaterThanOrEqualTo(dtStart), specification);
            }
            if (!StringUtils.isEmpty(endDate)) {
                LocalDateTime dateEnd = LocalDateTime.of(LocalDate.parse(endDate, dateTimeFormatter), LocalTime.of(23, 59, 59));
                dtEnd = dateEnd.toInstant(ZoneOffset.UTC);
                add(inventoryTransactionRepository.specialisationDateLessThanOrEqualTo(dtEnd), specification);
            }
        } else {
            add(inventoryTransactionRepository.specialisationDateMvt(dtStart, dtEnd), specification);
        }
        return inventoryTransactionRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "createdAt"))
            .stream().map(InventoryTransactionDTO::new).collect(Collectors.toList());
    }
}
