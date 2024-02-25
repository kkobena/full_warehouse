package com.kobe.warehouse.service.criteria;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.InventoryTransaction_;
import com.kobe.warehouse.domain.Produit_;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.service.dto.filter.InventoryTransactionFilterDTO;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InventoryTransactionSpec implements Specification<InventoryTransaction> {
  private InventoryTransactionFilterDTO inventoryTransactionFilter;

  public void setInventoryTransactionFilter(
      InventoryTransactionFilterDTO inventoryTransactionFilter) {
    this.inventoryTransactionFilter = inventoryTransactionFilter;
  }

  @Override
  public Predicate toPredicate(
      Root<InventoryTransaction> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
    List<Predicate> predicates = new ArrayList<>();

    if (Objects.nonNull(this.inventoryTransactionFilter.getType())
        && this.inventoryTransactionFilter.getType() != -1) {
      TransactionType transactionType =
          TransactionType.values()[this.inventoryTransactionFilter.getType()];
      predicates.add(
          criteriaBuilder.equal(root.get(InventoryTransaction_.transactionType), transactionType));
    }
    if (Objects.nonNull(this.inventoryTransactionFilter.getProduitId())) {
      predicates.add(
          criteriaBuilder.equal(
              root.get(InventoryTransaction_.produit).get(Produit_.id),
              this.inventoryTransactionFilter.getProduitId()));
    }
    LocalDateTime startAt = begin();
    LocalDateTime end = end();
    predicates.add(
        criteriaBuilder.between(root.get(InventoryTransaction_.createdAt), startAt, end));
    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
  }

  private LocalDateTime begin() {
    if (StringUtils.hasLength(this.inventoryTransactionFilter.getStartDate())) {
      return LocalDateTime.of(
          LocalDate.parse(
              this.inventoryTransactionFilter.getStartDate(),
              DateTimeFormatter.ofPattern("dd/MM/yyyy")),
          LocalTime.MIN);
    }

    return LocalDate.now().atStartOfDay();
  }

  private LocalDateTime end() {
    if (StringUtils.hasLength(this.inventoryTransactionFilter.getEndDate())) {

      return LocalDateTime.of(
          LocalDate.parse(
              this.inventoryTransactionFilter.getEndDate(),
              DateTimeFormatter.ofPattern("dd/MM/yyyy")),
          LocalTime.MAX);
    }
    return LocalDate.now().atTime(LocalTime.MAX);
  }
}
